package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.HealthcareService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthcareServiceRepository extends JpaRepository<HealthcareService, Long> {
    List<HealthcareService> findByOrgId(Long orgId);
    Optional<HealthcareService> findByIdAndOrgId(Long id, Long orgId);
}
