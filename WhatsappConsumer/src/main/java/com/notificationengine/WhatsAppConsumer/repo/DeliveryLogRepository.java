package com.notificationengine.WhatsAppConsumer.repo;

import com.notificationengine.WhatsAppConsumer.models.db.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

}

