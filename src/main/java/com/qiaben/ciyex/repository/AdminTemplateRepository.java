package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.AdminTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminTemplateRepository extends JpaRepository<AdminTemplate, Long> {

    @Query("SELECT t FROM AdminTemplate t WHERE t.orgId = :orgId")
    List<AdminTemplate> findAllByOrgId(Long orgId);

    boolean existsByTemplateId(String templateId);
}
