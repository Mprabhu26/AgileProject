package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUsernameAndReadFalse(String username);

    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    // ✅ ADD THIS
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);
}
