package com.notificationengine.common.repo;

import com.notificationengine.common.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

}

