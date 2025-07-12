package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    // --- Many-to-Many Org support ---
    // Find all users in a given org
    List<User> findByOrgs_Id(Long orgId);

    // Find user by email and org (user must belong to the org)
    Optional<User> findByEmailAndOrgs_Id(String email, Long orgId);

    // Check if user exists in a given org
    boolean existsByEmailAndOrgs_Id(String email, Long orgId);

    // Delete user from org (if needed)
    void deleteByEmailAndOrgs_Id(String email, Long orgId);


}
