package com.notificationengine.common.repo;

import com.notificationengine.common.model.Preference;
import com.notificationengine.common.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    Optional<Preference> findByUserIdAndChannel(Long userId, Channel channel);
}

