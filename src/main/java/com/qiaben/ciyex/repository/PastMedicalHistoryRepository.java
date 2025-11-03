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
//    List<PastMedicalHistory> findByPatientId(Long patientId);
//
//    List<PastMedicalHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<PastMedicalHistory> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PastMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastMedicalHistoryRepository extends JpaRepository<PastMedicalHistory, Long> {
    List<PastMedicalHistory> findByPatientId(Long patientId);

    List<PastMedicalHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);

    Optional<PastMedicalHistory> findByPatientIdAndEncounterIdAndId(
            Long patientId, Long encounterId, Long id
    );
}

