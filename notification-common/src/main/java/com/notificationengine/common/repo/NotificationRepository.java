package com.notificationengine.common.repo;

import com.notificationengine.common.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByProviderMessageSid(String providerMessageSid);
}