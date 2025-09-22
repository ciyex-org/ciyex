package com.qiaben.ciyex.repository.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qiaben.ciyex.entity.portal.entity.PortalUser;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {
    boolean existsByEmail(String email);
    Optional<PortalUser> findByEmail(String email);
}
