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
//    List<PatientMedicalHistory> findByPatientId(Long patientId);
//
//    List<PatientMedicalHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<PatientMedicalHistory> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, Long> {
    List<PatientMedicalHistory> findByPatientId(Long patientId);
    List<PatientMedicalHistory> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<PatientMedicalHistory> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
