package com.qiaben.ciyex.repository.portal;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qiaben.ciyex.entity.portal.PortalProfile;

import java.util.Optional;
import java.util.UUID;

public interface PortalProfileRepository extends JpaRepository<PortalProfile, Long> {
    Optional<PortalProfile> findByUserId(UUID userId);
}
//s