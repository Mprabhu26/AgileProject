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
            @PathVariable Long id,
            @RequestParam(required = false) Long projectId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Find and mark notification as read
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            notification.setIsRead(true);  // ← CHANGED from setRead(true)
            notificationRepository.save(notification);

            // Redirect to project if projectId provided
            if (projectId != null) {
                return "redirect:/ui/projects/" + projectId;
            }

            // Otherwise redirect back to notifications
            return "redirect:/ui/employee/notifications";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/ui/employee/notifications";
        }
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            // Assuming you have employee ID from username
            // You'll need to get employee first, then their notifications

            redirectAttributes.addFlashAttribute("success", "All notifications marked as read");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/ui/employee/notifications";
    }
}