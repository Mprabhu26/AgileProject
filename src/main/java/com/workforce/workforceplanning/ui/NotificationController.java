package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Notification;
import com.workforce.workforceplanning.repository.NotificationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/ui/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Mark notification as read and redirect to project
     */
    @GetMapping("/{id}/read")
    public String markAsReadAndRedirect(
            @PathVariable("id") String id,
            @RequestParam(value = "projectId", required = false) Long projectId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // ALWAYS try to get projectId from the ID parameter first
            Long actualProjectId = null;

            // If ID is "project-123" format
            if (id.startsWith("project-")) {
                actualProjectId = Long.parseLong(id.substring("project-".length()));
            }
            // If projectId parameter was provided
            else if (projectId != null) {
                actualProjectId = projectId;
            }

            // If we have a projectId, redirect to it
            if (actualProjectId != null) {
                return "redirect:/ui/projects/" + actualProjectId;
            }

            // Otherwise go to dashboard
            return "redirect:/ui/projects";

        } catch (Exception e) {
            return "redirect:/ui/projects";
        }
    }


    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            // Simple implementation - mark all user's notifications as read
            var notifications = notificationRepository.findByUsernameAndIsReadFalseOrderByCreatedAtDesc(username);
            for (Notification notification : notifications) {
                notification.setIsRead(true);
            }
            notificationRepository.saveAll(notifications);

            redirectAttributes.addFlashAttribute("success", "All notifications marked as read");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/ui/projects";
    }
}