//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.FamilyHistory;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface FamilyHistoryRepository extends JpaRepository<FamilyHistory, Long> {
//
//    List<FamilyHistory> findByPatientId(Long patientId);
//
//    List<FamilyHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<FamilyHistory> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.FamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyHistoryRepository extends JpaRepository<FamilyHistory, Long> {
    List<FamilyHistory> findByPatientId(Long patientId);

    List<FamilyHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);

    Optional<FamilyHistory> findByPatientIdAndEncounterIdAndId(
            Long patientId, Long encounterId, Long id
    );
}
