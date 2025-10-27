package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
    Immunization findOneByIdAndPatientId(Long immunizationId, Long patientId);

    List<Immunization> findByPatientId(Long patientId);
}
