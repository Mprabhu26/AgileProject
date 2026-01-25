package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Custom query methods
    List<Notification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<Notification> findByEmployeeIdAndIsReadFalseOrderByCreatedAtDesc(Long employeeId);

    long countByEmployeeIdAndIsReadFalse(Long employeeId);

    boolean existsByEmployeeIdAndIsReadFalse(Long employeeId);

    // methods for username-based notifications:
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    List<Notification> findByUsernameAndIsReadFalseOrderByCreatedAtDesc(String username);

    long countByUsernameAndIsReadFalse(String username);
}