package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    Optional<Slot> findByExternalId(String externalId);

    @Query("SELECT s FROM Slot s WHERE s.orgId = :orgId")
    List<Slot> findAllByOrgId(Long orgId);
}