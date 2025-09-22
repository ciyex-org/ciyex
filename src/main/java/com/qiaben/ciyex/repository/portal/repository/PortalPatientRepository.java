package com.qiaben.ciyex.repository.portal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.qiaben.ciyex.entity.portal.entity.PortalPatient;

@Repository
public interface PortalPatientRepository extends JpaRepository<PortalPatient, Long> {

    /**
     * Find a patient by the linked PortalUser.id
     */
    Optional<PortalPatient> findByUser_Id(Long userId);

    /**
     * Check if a patient exists for a given user
     */
    boolean existsByUser_Id(Long userId);
}
