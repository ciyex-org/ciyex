package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("SELECT COUNT(s) FROM Slot s WHERE s.orgId = :orgId")
    long countByOrgId(Long orgId);

    List<Slot> findAllByOrgId(Long orgId);
}
