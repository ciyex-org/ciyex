//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.PhysicalExam;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PhysicalExamRepository extends JpaRepository<PhysicalExam, Long> {
//
//    List<PhysicalExam> findByPatientId(Long patientId);
//
//    List<PhysicalExam> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<PhysicalExam> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PhysicalExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhysicalExamRepository extends JpaRepository<PhysicalExam, Long> {
    List<PhysicalExam> findByPatientId(Long patientId);
    List<PhysicalExam> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<PhysicalExam> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
