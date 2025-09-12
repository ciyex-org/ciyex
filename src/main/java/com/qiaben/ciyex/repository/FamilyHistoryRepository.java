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
//    List<FamilyHistory> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<FamilyHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<FamilyHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}

package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.FamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyHistoryRepository extends JpaRepository<FamilyHistory, Long> {

    List<FamilyHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);

    Optional<FamilyHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
            Long orgId, Long patientId, Long encounterId, Long id
    );
}
