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
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt);

    @Query(value = """
        SELECT *
        FROM coverages
        """, nativeQuery = true)
    List<Coverage> findAllByText();

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(id     AS TEXT) = :idTxt
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByIdText(@Param("idTxt") String idTxt);

    @Query(value = """
        SELECT *
        FROM coverages
        WHERE CAST(id         AS TEXT) = :idTxt
          AND CAST(patient_id AS TEXT) = :patientIdTxt
        LIMIT 1
        """, nativeQuery = true)
    Optional<Coverage> findByIdAndPatientIdAndOrgIdText(@Param("idTxt") String idTxt,
                                                        @Param("patientIdTxt") String patientIdTxt);

    // keep if needed elsewhere (e.g., sync flows)
    Optional<Coverage> findByExternalId(String externalId);
}
