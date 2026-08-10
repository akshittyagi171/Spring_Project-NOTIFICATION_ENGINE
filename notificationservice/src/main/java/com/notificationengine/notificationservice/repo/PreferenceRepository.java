package com.notificationengine.notificationservice.repo;

import com.notificationengine.notificationservice.models.db.Preference;
import com.notificationengine.common.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    List<Preference> findByUserId(Long userId);

    Optional<Preference> findByUserIdAndChannel(Long userId, Channel channel);
}

