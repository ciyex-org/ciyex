package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    // REMOVED: Optional<LabOrder> findByExternalId(String externalId);

    @Query("SELECT o FROM LabOrder o WHERE o.orgId = :orgId")
    List<LabOrder> findAllByOrgId(Long orgId);

    // Multi-org list
    @Query("SELECT o FROM LabOrder o WHERE o.orgId IN :orgIds")
    List<LabOrder> findAllByOrgIdIn(Collection<Long> orgIds);

    // Patient-scoped list (multi-org aware)
    @Query("SELECT o FROM LabOrder o WHERE o.patientId = :patientId AND o.orgId IN :orgIds")
    List<LabOrder> findAllByPatientIdInOrgs(Long patientId, Collection<Long> orgIds);

    // Patient-scoped fetch single
    Optional<LabOrder> findByIdAndPatientId(Long id, Long patientId);
}
