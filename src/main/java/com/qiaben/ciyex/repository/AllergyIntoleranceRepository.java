package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.AllergyIntolerance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AllergyIntoleranceRepository extends JpaRepository<AllergyIntolerance, Long> {

    // Single-tenant methods
    List<AllergyIntolerance> findAllByPatientId(Long patientId);

    @Query(value = """
        SELECT *
        FROM allergy_intolerances
        WHERE CAST(patient_id AS TEXT) = :patientIdTxt
        ORDER BY id
        """, nativeQuery = true)
    List<AllergyIntolerance> findAllByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt);

    @Modifying
    @Query(value = "DELETE FROM allergy_intolerances WHERE patient_id = :patientId", nativeQuery = true)
    int deleteAllByPatientId(@Param("patientId") Long patientId);

    @Modifying
    @Query(value = """
        DELETE FROM allergy_intolerances
        WHERE CAST(patient_id AS TEXT) = :patientIdTxt
        """, nativeQuery = true)
    int deleteAllByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt);

    @Modifying
    @Query(value = "DELETE FROM allergy_intolerances WHERE id = :id AND patient_id = :patientId", nativeQuery = true)
    int deleteOneByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    @Modifying
    @Query(value = """
        DELETE FROM allergy_intolerances
        WHERE CAST(id        AS TEXT) = :idTxt
          AND CAST(patient_id AS TEXT) = :patientIdTxt
        """, nativeQuery = true)
    int deleteOneByIdAndPatientIdAndOrgIdText(@Param("idTxt") String idTxt,
                                              @Param("patientIdTxt") String patientIdTxt);

    @Query(value = "SELECT * FROM allergy_intolerances ORDER BY patient_id, id", nativeQuery = true)
    List<AllergyIntolerance> findAllOrderedByPatient();

    @Query(value = """
        SELECT *
        FROM allergy_intolerances
        ORDER BY patient_id, id
        """, nativeQuery = true)
    List<AllergyIntolerance> findByText();
}
