package org.ciyex.ehr.immunization.repository;

import org.ciyex.ehr.immunization.entity.ImmunizationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImmunizationRepository extends JpaRepository<ImmunizationRecord, Long> {

    Page<ImmunizationRecord> findByOrgAlias(String orgAlias, Pageable pageable);

    List<ImmunizationRecord> findByOrgAliasAndPatientIdOrderByAdministrationDateDesc(String orgAlias, Long patientId);

    List<ImmunizationRecord> findByOrgAliasAndCvxCodeOrderByAdministrationDateDesc(String orgAlias, String cvxCode);

    List<ImmunizationRecord> findByOrgAliasAndPatientIdAndCvxCodeOrderByAdministrationDateDesc(
            String orgAlias, Long patientId, String cvxCode);

    long countByOrgAliasAndPatientId(String orgAlias, Long patientId);

    long countByOrgAliasAndPatientIdAndStatus(String orgAlias, Long patientId, String status);

    @Query("SELECT i FROM ImmunizationRecord i WHERE i.orgAlias = :org AND i.patientId = :pid AND (" +
           "LOWER(i.vaccineName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.cvxCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.manufacturer) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.administeredBy) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.lotNumber) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY i.administrationDate DESC")
    List<ImmunizationRecord> searchByPatient(@Param("org") String orgAlias, @Param("pid") Long patientId, @Param("q") String query);

    @Query("SELECT i FROM ImmunizationRecord i WHERE i.orgAlias = :org AND (" +
           "LOWER(i.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.vaccineName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.cvxCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.manufacturer) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(i.administeredBy) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(i.patientId AS text) = :q" +
           ") ORDER BY i.administrationDate DESC")
    List<ImmunizationRecord> search(@Param("org") String orgAlias, @Param("q") String query);
}
