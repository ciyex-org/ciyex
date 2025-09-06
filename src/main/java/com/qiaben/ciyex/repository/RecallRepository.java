package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Recall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecallRepository extends JpaRepository<Recall, Long> {
    List<Recall> findByOrgId(Long orgId);
    List<Recall> findByPatientId(Long patientId);

    @Query("SELECT COUNT(r) FROM Recall r WHERE r.orgId = :orgId")
    long countByOrgId(Long orgId);

    Page<Recall> findAllByOrgId(Long orgId, Pageable pageable);

}
