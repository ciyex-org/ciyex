package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PractitionerRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PractitionerRoleRepository extends JpaRepository<PractitionerRole, Long> {
    // Custom queries can be added here if needed
}
