//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Assessment;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
//
//    List<Assessment> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<Assessment> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<Assessment> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByOrgIdAndPatientId(Long orgId, Long patientId);
    List<Assessment> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<Assessment> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
