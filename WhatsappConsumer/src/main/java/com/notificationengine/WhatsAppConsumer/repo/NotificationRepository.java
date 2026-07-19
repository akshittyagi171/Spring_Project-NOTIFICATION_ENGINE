package com.notificationengine.WhatsAppConsumer.repo;

import com.notificationengine.WhatsAppConsumer.models.db.Notification;
import com.notificationengine.WhatsAppConsumer.models.enums.Channel;
import com.notificationengine.WhatsAppConsumer.models.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

}

