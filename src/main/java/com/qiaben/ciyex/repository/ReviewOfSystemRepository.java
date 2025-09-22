//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.ReviewOfSystem;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface ReviewOfSystemRepository extends JpaRepository<ReviewOfSystem, Long> {
//
//    List<ReviewOfSystem> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<ReviewOfSystem> findByOrgIdAndPatientIdAndEncounterId(
//            Long orgId, Long patientId, Long encounterId
//    );
//
//    Optional<ReviewOfSystem> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ReviewOfSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewOfSystemRepository extends JpaRepository<ReviewOfSystem, Long> {
    List<ReviewOfSystem> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<ReviewOfSystem> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
