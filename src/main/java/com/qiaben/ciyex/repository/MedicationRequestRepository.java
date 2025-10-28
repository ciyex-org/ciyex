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
    
    @Query(value = "SELECT pp.ehr_patient_id FROM public.portal_patients pp " +
                   "JOIN public.portal_users pu ON pp.portal_user_id = pu.id " +
                   "WHERE pu.keycloak_user_id = :keycloakUserId LIMIT 1", nativeQuery = true)
    Long findEhrPatientIdByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);
}
