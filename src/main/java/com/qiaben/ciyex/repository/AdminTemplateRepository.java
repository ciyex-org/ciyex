package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.AdminTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminTemplateRepository extends JpaRepository<AdminTemplate, Long> {

    List<AdminTemplate> findAllByOrgId(Long orgId);

}
