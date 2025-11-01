//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.AssignedProvider;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface AssignedProviderRepository extends JpaRepository<AssignedProvider, Long> {
//
//    List<AssignedProvider> findByPatientId(Long patientId);
//
//    List<AssignedProvider> findByPatientIdAndEncounterId(
//            Long patientId, Long encounterId);
//
//    Optional<AssignedProvider> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id);
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.AssignedProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignedProviderRepository extends JpaRepository<AssignedProvider, Long> {
    List<AssignedProvider> findByPatientId(Long patientId);

    List<AssignedProvider> findByPatientIdAndEncounterId(Long patientId, Long encounterId);

    Optional<AssignedProvider> findByPatientIdAndEncounterIdAndId(
            Long patientId, Long encounterId, Long id
    );
}
