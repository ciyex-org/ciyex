package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {

    @Query("SELECT i FROM Immunization i WHERE i.patientId = :patientId AND i.orgId = :orgId")
    List<Immunization> findByPatientIdAndOrgId(Long patientId, Long orgId);

    @Query("SELECT i FROM Immunization i WHERE i.id = :id AND i.patientId = :patientId AND i.orgId = :orgId")
    Optional<Immunization> findOneByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);

    @Query("SELECT i FROM Immunization i WHERE i.orgId = :orgId")
    List<Immunization> findAllByOrgId(Long orgId);
}
