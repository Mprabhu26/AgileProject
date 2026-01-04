package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/projects")
public class ProjectPublishController {

    private final ProjectRepository projectRepository;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService; // ADD THIS

    public ProjectPublishController(
            ProjectRepository projectRepository,
            RuntimeService runtimeService,
            TaskService taskService,
            RepositoryService repositoryService) { // ADD THIS PARAMETER
        this.projectRepository = projectRepository;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService; // INITIALIZE IT
    }

    // ========================
    // PUBLISH PROJECT
    // ========================
    @PostMapping("/{id}/publish")
    public String publishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        return projectRepository.findById(id)
                .map(project -> {

                    // 1. Mark project as published
                    project.setPublished(true);
                    project.setVisibleToAll(true);
                    project.setPublishedAt(LocalDateTime.now());

                    // 2. Move project into STAFFING phase
                    if (project.getStatus() == ProjectStatus.PENDING
                            || project.getStatus() == ProjectStatus.APPROVED) {
                        project.setStatus(ProjectStatus.STAFFING);
                    }

                    // 3. Workflow is about to start → mark as RUNNING
                    project.setWorkflowStatus("RUNNING");

                    // 4. Save BEFORE starting workflow (important!)
                    projectRepository.save(project);

                    // 5. Start Flowable workflow
                    triggerWorkflowForProject(project);

                    redirectAttributes.addFlashAttribute(
                            "successMessage",
                            "Project '" + project.getName() + "' published successfully! Workflow started."
                    );

                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }

    // ========================
    // START WORKFLOW (SIMPLIFIED VERSION)
    // ========================
    private void triggerWorkflowForProject(Project project) {
        try {
            System.out.println("🚀 Starting workflow for project: " + project.getName());

            // Update database BEFORE starting workflow
            project.setWorkflowStatus("RUNNING");
            projectRepository.save(project); // ✅ Using YOUR ProjectRepository here

            System.out.println("✅ Updated project workflow status to RUNNING");

            // Check if process definition exists
            var processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("workforcePlanningProcess")
                    .latestVersion()
                    .singleResult();

            if (processDefinition == null) {
                throw new RuntimeException("Process definition 'workforcePlanningProcess' not found!");
            }

            System.out.println("✅ Found process definition: " + processDefinition.getName());

            // Create workflow variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", project.getId());
            variables.put("projectName", project.getName());
            variables.put("projectDescription", project.getDescription());
            variables.put("createdBy", project.getCreatedBy());
            variables.put("budget", project.getBudget());
            variables.put("totalEmployeesRequired", project.getTotalEmployeesRequired());
            variables.put("startDate", project.getStartDate().toString());
            variables.put("endDate", project.getEndDate().toString());

            // Start Flowable process
            var processInstance = runtimeService.startProcessInstanceByKey(
                    "workforcePlanningProcess",
                    project.getId().toString(),
                    variables
            );

            // Link workflow to project
            project.setProcessInstanceId(processInstance.getId());
            projectRepository.save(project); // ✅ Using YOUR ProjectRepository here

            System.out.println("✅ Workflow started successfully");
            System.out.println("   Process Instance ID: " + processInstance.getId());
            System.out.println("   Process Definition ID: " + processDefinition.getId());

        } catch (Exception e) {
            System.err.println("❌ Failed to start workflow for project " + project.getId());
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();

            project.setWorkflowStatus("FAILED");
            projectRepository.save(project); // ✅ Using YOUR ProjectRepository here
        }
    }

    // ========================
    // UNPUBLISH PROJECT (Fixed to go to PENDING)
    // ========================
    @PostMapping("/{id}/unpublish")
    public String unpublishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        return projectRepository.findById(id)
                .map(project -> {

                    String processInstanceId = project.getProcessInstanceId();

                    // 1. Roll back business status - FIXED: Go to PENDING, not APPROVED
                    if (project.getStatus() == ProjectStatus.STAFFING) {
                        project.setStatus(ProjectStatus.PENDING); // CHANGED FROM APPROVED
                    } else if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                        project.setStatus(ProjectStatus.PENDING);
                    }else if (project.getStatus() == ProjectStatus.APPROVED) {
                        project.setStatus(ProjectStatus.PENDING);
                    }

                    // 2. Unpublish flags
                    project.setPublished(false);
                    project.setVisibleToAll(false);

                    // 3. Workflow is being cancelled
                    project.setWorkflowStatus("CANCELLED");

                    // 4. Persist state
                    projectRepository.save(project);

                    // 5. Notify workflow if still running
                    if (processInstanceId != null) {
                        handleWorkflowOnUnpublish(processInstanceId, project);
                    }

                    redirectAttributes.addFlashAttribute(
                            "successMessage",
                            "Project '" + project.getName() + "' unpublished successfully!"
                    );

                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }

    // ========================
    // HANDLE UNPUBLISH MESSAGE
    // ========================
    private void handleWorkflowOnUnpublish(String processInstanceId, Project project) {
        try {
            System.out.println("🛑 Sending unpublish message to workflow for project: " + project.getName());

            var processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null || processInstance.isEnded()) {
                System.out.println("ℹ️ Workflow already ended");
                return;
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", project.getId());
            variables.put("projectName", project.getName());
            variables.put("unpublishReason", "Project unpublished by manager");
            variables.put("unpublishTime", LocalDateTime.now().toString());

            runtimeService.messageEventReceived(
                    "projectUnpublishedMsg",
                    processInstanceId,
                    variables
            );

            System.out.println("✅ Unpublish message sent to workflow");

        } catch (Exception e) {
            System.err.println("❌ Error sending unpublish message: " + e.getMessage());
        }
    }
}