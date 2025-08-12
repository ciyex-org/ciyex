package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.MedicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicationRequestRepository extends JpaRepository<MedicationRequest, Long> {
    List<MedicationRequest> findByPatientId(Long patientId);
    List<MedicationRequest> findByEncounterId(Long encounterId);
    List<MedicationRequest> findByPatientIdOrEncounterId(Long patientId, Long encounterId);
}
