package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    // Keep if policy number column is text in DB
    Optional<Coverage> findByPolicyNumber(String policyNumber);

    // -------- Native queries with CAST(... AS TEXT) to avoid bigint/text mismatch --------

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(patient_id AS TEXT) = :patientIdTxt
          AND CAST(org_id     AS TEXT) = :orgIdTxt
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt,
                                                   @Param("orgIdTxt") String orgIdTxt);

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(org_id AS TEXT) = :orgIdTxt
        """, nativeQuery = true)
    List<Coverage> findAllByOrgIdText(@Param("orgIdTxt") String orgIdTxt);

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(id     AS TEXT) = :idTxt
          AND CAST(org_id AS TEXT) = :orgIdTxt
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByIdAndOrgIdText(@Param("idTxt") String idTxt,
                                            @Param("orgIdTxt") String orgIdTxt);

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(id         AS TEXT) = :idTxt
          AND CAST(patient_id AS TEXT) = :patientIdTxt
          AND CAST(org_id     AS TEXT) = :orgIdTxt
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByIdAndPatientIdAndOrgIdText(@Param("idTxt") String idTxt,
                                                        @Param("patientIdTxt") String patientIdTxt,
                                                        @Param("orgIdTxt") String orgIdTxt);

    // keep if needed elsewhere (e.g., sync flows)
    Optional<Coverage> findByExternalId(String externalId);

    // For portal patients to see their coverages
    List<Coverage> findByPatientIdOrderByEffectiveDateDesc(Long patientId);
}
