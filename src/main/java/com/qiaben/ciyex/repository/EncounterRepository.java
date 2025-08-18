package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    // Additional query methods can be added here if necessary
}
