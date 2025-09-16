//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.SocialHistory;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SocialHistoryRepository extends JpaRepository<SocialHistory, Long> {
//
//    List<SocialHistory> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<SocialHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<SocialHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}


package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.SocialHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialHistoryRepository extends JpaRepository<SocialHistory, Long> {
    List<SocialHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<SocialHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
