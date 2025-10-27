package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    // REMOVED: Optional<LabOrder> findByExternalId(String externalId);

    // Single tenant - no orgId filtering needed
    
    List<LabOrder> findAllByPatientId(Long patientId);

    // Patient-scoped fetch single
    Optional<LabOrder> findByIdAndPatientId(Long id, Long patientId);
}
