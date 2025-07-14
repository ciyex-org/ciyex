package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByOrg_Id(Long orgId);
}
