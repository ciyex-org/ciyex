package org.ciyex.ehr.lab.repository;

import org.ciyex.ehr.lab.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {
    List<LabOrder> findByOrgAliasAndPatientIdOrderByOrderDateDesc(String orgAlias, Long patientId);

    List<LabOrder> findByOrgAliasOrderByCreatedAtDesc(String orgAlias);

    @Query("SELECT o FROM LabOrder o WHERE o.orgAlias = :org AND (" +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(o.orderName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(o.testCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(o.physicianName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(o.orderingProvider) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(o.patientId AS text) = :q" +
           ") ORDER BY o.createdAt DESC")
    List<LabOrder> search(@Param("org") String orgAlias, @Param("q") String query);

    long countByOrgAlias(String orgAlias);
}
