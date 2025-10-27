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
//    List<Assessment> findByPatientId(Long patientId);
//
//    List<Assessment> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<Assessment> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    // Single tenant per instance - no orgId filtering needed
    List<Assessment> findByPatientId(Long patientId);
    List<Assessment> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<Assessment> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
