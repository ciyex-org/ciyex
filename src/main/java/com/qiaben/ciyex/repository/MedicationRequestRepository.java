package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.MedicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MedicationRequestRepository extends JpaRepository<MedicationRequest, Long> {
    List<MedicationRequest> findByPatientId(Long patientId);
    List<MedicationRequest> findByEncounterId(Long encounterId);
    List<MedicationRequest> findByPatientIdOrEncounterId(Long patientId, Long encounterId);
    
    @Query("SELECT pp.ehrPatientId FROM PortalPatient pp WHERE pp.portalUser.uuid = :uuid")
    Long findEhrPatientIdByKeycloakUserId(@Param("uuid") java.util.UUID uuid);
}
