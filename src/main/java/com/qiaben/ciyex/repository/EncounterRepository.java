package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByOrgId(Long orgId);
    // Additional query methods can be added here if necessary
}
