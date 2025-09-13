package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    boolean existsByName(String name);
}
