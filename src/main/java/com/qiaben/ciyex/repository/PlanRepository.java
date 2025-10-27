//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Plan;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PlanRepository extends JpaRepository<Plan, Long> {
//    List<Plan> findByPatientId(Long patientId);
//    List<Plan> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//    Optional<Plan> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<Plan> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
