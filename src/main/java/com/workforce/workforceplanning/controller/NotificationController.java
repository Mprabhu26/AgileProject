package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.repository.NotificationRepository;
import com.workforce.workforceplanning.model.Notification;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 🔔 Bell count
    @GetMapping("/count")
    public long getUnreadCount(@RequestParam String username) {
        return notificationRepository.countByUsernameAndReadFalse(username);
    }

    // 📜 List notifications
    @GetMapping
    public List<Notification> getNotifications(@RequestParam String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    // ✅ Mark as read
    @PostMapping("/{id}/read")
    public String markAsReadAndRedirect(
            @PathVariable Long id,
            @RequestParam Long projectId
    ) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notificationRepository.save(notification);

        return "redirect:/ui/projects/" + projectId;
    }

}
