package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientEducation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientEducationRepository extends JpaRepository<PatientEducation, Long> {
    Page<PatientEducation> findAllByOrgId(Long orgId, Pageable pageable);
}
