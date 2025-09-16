//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.PatientMedicalHistory;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, Long> {
//
//    List<PatientMedicalHistory> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<PatientMedicalHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//
//    Optional<PatientMedicalHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id
//    );
//}



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, Long> {
    List<PatientMedicalHistory> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<PatientMedicalHistory> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
