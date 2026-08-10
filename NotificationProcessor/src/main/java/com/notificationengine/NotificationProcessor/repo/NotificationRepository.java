package com.notificationengine.NotificationProcessor.repo;

import com.notificationengine.NotificationProcessor.models.db.Notification;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndChannel(Long userId, Channel channel);

    List<Notification> findByStatus(Status status);
}

