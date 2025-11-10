package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRelationshipRepository extends JpaRepository<PatientRelationship, Long> {

    List<PatientRelationship> findByOrgIdAndPatientId(Long orgId, Long patientId);

    Optional<PatientRelationship> findByIdAndOrgId(Long id, Long orgId);

    List<PatientRelationship> findByPatientId(Long patientId);
}