package com.notificationengine.PushNConsumer.repo;

import com.notificationengine.PushNConsumer.models.db.Notification;
import com.notificationengine.PushNConsumer.models.enums.Channel;
import com.notificationengine.PushNConsumer.models.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndChannel(Long userId, Channel channel);

    List<Notification> findByStatus(Status status);
}

