package com.qiaben.ciyex.repository.portal;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qiaben.ciyex.entity.portal.PortalProfile;

import java.util.Optional;

public interface PortalProfileRepository extends JpaRepository<PortalProfile, Long> {
    Optional<PortalProfile> findByUserId(Long userId);
}
