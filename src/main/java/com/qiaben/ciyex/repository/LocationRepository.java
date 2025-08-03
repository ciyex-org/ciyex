package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByExternalId(String externalId);

    @Query("SELECT l.externalId FROM Location l WHERE l.orgId = :orgId")
    List<String> findAllExternalIdsByOrgId(Long orgId);

    @Query("SELECT l FROM Location l WHERE l.orgId = :orgId")
    List<Location> findAllByOrgId(Long orgId);
}