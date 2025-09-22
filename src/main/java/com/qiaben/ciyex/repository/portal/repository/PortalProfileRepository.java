package com.qiaben.ciyex.repository.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qiaben.ciyex.entity.portal.entity.PortalProfile;

import java.util.Optional;

public interface PortalProfileRepository extends JpaRepository<PortalProfile, Long> {
    Optional<PortalProfile> findByUserId(Long userId);
}
