package com.notificationengine.SMSConsumer.repo;


import com.notificationengine.SMSConsumer.models.db.DeliveryLog;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    List<DeliveryLog> findByNotificationId(Long notificationId);

    List<DeliveryLog> findByChannel(Channel channel);

    List<DeliveryLog> findByStatus(Status status);
}

