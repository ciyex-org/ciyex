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
//    List<ReviewOfSystem> findByPatientId(Long patientId);
//
//    List<ReviewOfSystem> findByPatientIdAndEncounterId(
//            Long patientId, Long encounterId
//    );
//
//    Optional<ReviewOfSystem> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ReviewOfSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewOfSystemRepository extends JpaRepository<ReviewOfSystem, Long> {
    List<ReviewOfSystem> findByPatientId(Long patientId);
    List<ReviewOfSystem> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<ReviewOfSystem> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
