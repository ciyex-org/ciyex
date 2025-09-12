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
//    List<PhysicalExam> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<PhysicalExam> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<PhysicalExam> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}

package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PhysicalExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhysicalExamRepository extends JpaRepository<PhysicalExam, Long> {
    List<PhysicalExam> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<PhysicalExam> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
