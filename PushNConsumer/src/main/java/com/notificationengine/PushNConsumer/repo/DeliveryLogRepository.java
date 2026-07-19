package com.notificationengine.PushNConsumer.repo;


import com.notificationengine.PushNConsumer.models.db.DeliveryLog;
import com.notificationengine.PushNConsumer.models.enums.Channel;
import com.notificationengine.PushNConsumer.models.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    List<DeliveryLog> findByNotificationId(Long notificationId);

    List<DeliveryLog> findByChannel(Channel channel);

    List<DeliveryLog> findByStatus(Status status);
}

