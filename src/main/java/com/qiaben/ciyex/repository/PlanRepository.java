//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Plan;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PlanRepository extends JpaRepository<Plan, Long> {
//    List<Plan> findByOrgIdAndPatientId(Long orgId, Long patientId);
//    List<Plan> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//    Optional<Plan> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<Plan> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
