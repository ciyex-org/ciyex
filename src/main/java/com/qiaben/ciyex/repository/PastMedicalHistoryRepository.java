//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.PastMedicalHistory;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PastMedicalHistoryRepository extends JpaRepository<PastMedicalHistory, Long> {
//
//    List<PastMedicalHistory> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<PastMedicalHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<PastMedicalHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PastMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastMedicalHistoryRepository extends JpaRepository<PastMedicalHistory, Long> {

    List<PastMedicalHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);

    Optional<PastMedicalHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
            Long orgId, Long patientId, Long encounterId, Long id
    );
}

