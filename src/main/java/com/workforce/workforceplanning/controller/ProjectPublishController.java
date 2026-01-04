package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
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

    public ProjectPublishController(
            ProjectRepository projectRepository,
            RuntimeService runtimeService,
            TaskService taskService) {
        this.projectRepository = projectRepository;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @PostMapping("/{id}/publish")
    public String publishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        return projectRepository.findById(id)
                .map(project -> {
                    // 1. Update project status to published
                    project.setPublished(true);
                    project.setVisibleToAll(true);
                    project.setPublishedAt(LocalDateTime.now());

                    // 2. Change project status to STAFFING (for Resource Planner)
                    if (project.getStatus() == ProjectStatus.PENDING ||
                            project.getStatus() == ProjectStatus.APPROVED) {
                        project.setStatus(ProjectStatus.STAFFING);
                    }

                    // 3. Trigger workflow
                    triggerWorkflowForProject(project);

                    // 4. Save project (workflow status is saved inside triggerWorkflowForProject)
                    projectRepository.save(project);

                    // 5. Add success message
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Project '" + project.getName() + "' published successfully! Workflow started.");

                    // 6. Redirect back to projects list
                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }

    @PostMapping("/{id}/unpublish")
    public String unpublishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        return projectRepository.findById(id)
                .map(project -> {
                    // 1. Get workflow process instance ID
                    String processInstanceId = project.getProcessInstanceId();

                    // 2. Update project status based on current state
                    if (project.getStatus() == ProjectStatus.STAFFING) {
                        project.setStatus(ProjectStatus.APPROVED);
                    } else if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                        project.setStatus(ProjectStatus.CANCELLED);
                    }

                    // 3. Unpublish the project
                    project.setPublished(false);
                    project.setVisibleToAll(false);

                    // 4. Update workflow status
                    project.setWorkflowStatus("CANCELLED");

                    // 5. Save project first
                    projectRepository.save(project);

                    // 6. Handle workflow termination if active
                    if (processInstanceId != null) {
                        handleWorkflowOnUnpublish(processInstanceId, project);
                    }

                    // 7. Add success message
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Project '" + project.getName() + "' unpublished successfully!");

                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }

    // ========================
    // PRIVATE HELPER METHODS
    // ========================

    private void triggerWorkflowForProject(Project project) {
        try {
            System.out.println("🚀 Starting workflow for project: " + project.getName());

            // Create workflow variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", project.getId());
            variables.put("projectName", project.getName());
            variables.put("projectDescription", project.getDescription());
            variables.put("createdBy", project.getCreatedBy());
            variables.put("budget", project.getBudget());
            variables.put("totalEmployeesRequired", project.getTotalEmployeesRequired());
            variables.put("startDate", project.getStartDate());
            variables.put("endDate", project.getEndDate());

            // Add skill requirements
            StringBuilder skillsBuilder = new StringBuilder();
            for (var req : project.getSkillRequirements()) {
                skillsBuilder.append(req.getSkill())
                        .append(" (")
                        .append(req.getRequiredCount())
                        .append("), ");
            }
            if (skillsBuilder.length() > 0) {
                skillsBuilder.setLength(skillsBuilder.length() - 2);
            }
            variables.put("requiredSkills", skillsBuilder.toString());

            // Start the workflow process
            var processInstance = runtimeService.startProcessInstanceByKey(
                    "workforcePlanningProcess",
                    project.getId().toString(),
                    variables
            );

            // Save process instance ID to project
            project.setProcessInstanceId(processInstance.getId());
            project.setWorkflowStatus("STARTED");

            // Save the project with workflow info
            projectRepository.save(project);

            System.out.println("✅ Workflow started successfully!");
            System.out.println("   Process Instance ID: " + processInstance.getId());
            System.out.println("   Project ID: " + project.getId());

        } catch (Exception e) {
            System.err.println("❌ Failed to start workflow for project " + project.getId());
            System.err.println("   Error: " + e.getMessage());

            // Mark workflow as failed
            project.setWorkflowStatus("FAILED");
            projectRepository.save(project);
        }
    }

    private void handleWorkflowOnUnpublish(String processInstanceId, Project project) {
        try {
            System.out.println("🛑 Sending unpublish message to workflow for project: " + project.getName());

            // Check if process instance exists
            var processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null || processInstance.isEnded()) {
                System.out.println("ℹ️ Workflow already ended or doesn't exist");
                return;
            }

            // Create variables for the message
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", project.getId());
            variables.put("projectName", project.getName());
            variables.put("unpublishReason", "Project unpublished by manager");
            variables.put("unpublishTime", LocalDateTime.now().toString());

            // Send message to trigger unpublishing in workflow
            runtimeService.messageEventReceived(
                    "projectUnpublishedMsg",  // Must match BPMN message name
                    processInstanceId,
                    variables
            );

            System.out.println("✅ Message 'projectUnpublishedMsg' sent to workflow");

        } catch (Exception e) {
            System.err.println("❌ Error sending unpublish message: " + e.getMessage());
            // Continue anyway - project should still be unpublished
        }
    }
}