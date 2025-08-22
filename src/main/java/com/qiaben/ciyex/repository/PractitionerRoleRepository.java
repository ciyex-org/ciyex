package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PractitionerRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PractitionerRoleRepository extends JpaRepository<PractitionerRole, Long> {

    // Custom queries can beList<PractitionerRole> findByOrgId(Long orgId); added here if needed
    List<PractitionerRole> findByOrgId(Long orgId);
}

