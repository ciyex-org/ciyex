package com.qiaben.ciyex.repository.portal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.qiaben.ciyex.entity.portal.PortalUser;

import java.util.Optional;

@Repository
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {
    boolean existsByEmail(String email);

    Optional<PortalUser> findByEmail(String email);

    // 🔹 Case-insensitive search
    Optional<PortalUser> findByEmailIgnoreCase(String email);

    // 🔹 Prefix match (for handling alice@example.com → alice.doe@example.com)
    Optional<PortalUser> findFirstByEmailStartingWith(String prefix);
}