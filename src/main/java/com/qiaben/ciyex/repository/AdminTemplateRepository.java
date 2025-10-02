package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.AdminTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTemplateRepository extends JpaRepository<AdminTemplate, Long> {
    // repository for AdminTemplate does not filter by orgId anymore
    // use built-in findAll() / findAll(Pageable) from JpaRepository
}
