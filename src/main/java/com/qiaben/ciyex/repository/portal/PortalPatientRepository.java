package com.qiaben.ciyex.repository.portal;

import com.qiaben.ciyex.entity.portal.PortalPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortalPatientRepository extends JpaRepository<PortalPatient, Long> {

    /**
     * Find a patient by the linked PortalUser ID
     */
    Optional<PortalPatient> findByPortalUser_Id(Long portalUserId);

    /**
     * Find a patient by the linked PortalUser UUID
     */
    Optional<PortalPatient> findByPortalUser_Uuid(java.util.UUID portalUserUuid);

    /**
     * Find a patient by EHR patient ID
     */
    Optional<PortalPatient> findByEhrPatientId(Long ehrPatientId);

    /**
     * Check if a patient exists for a given portal user
     */
    boolean existsByPortalUser_Id(Long portalUserId);

    /**
     * Find all patients with approved portal users
     */
    @Query("SELECT pp FROM PortalPatient pp WHERE pp.portalUser.status = 'APPROVED'")
    List<PortalPatient> findApprovedPatients();

    /**
     * Find all patients with pending portal users
     */
    @Query("SELECT pp FROM PortalPatient pp WHERE pp.portalUser.status = 'PENDING'")
    List<PortalPatient> findPendingPatients();

    /**
     * Find patients by medical record number
     */
    Optional<PortalPatient> findByMedicalRecordNumber(String medicalRecordNumber);
}
