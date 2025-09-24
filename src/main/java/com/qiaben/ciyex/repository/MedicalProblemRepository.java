package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.MedicalProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalProblemRepository extends JpaRepository<MedicalProblem, Long> {

    @Query(value = """
        SELECT * FROM medical_problems
        WHERE CAST(patient_id AS TEXT) = :patientIdTxt
          AND CAST(org_id     AS TEXT) = :orgIdTxt
        ORDER BY id
        """, nativeQuery = true)
    List<MedicalProblem> findAllByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt,
                                                        @Param("orgIdTxt") String orgIdTxt);

    @Modifying
    @Query(value = """
        DELETE FROM medical_problems
        WHERE CAST(patient_id AS TEXT) = :patientIdTxt
          AND CAST(org_id     AS TEXT) = :orgIdTxt
        """, nativeQuery = true)
    int deleteAllByPatientIdAndOrgIdText(@Param("patientIdTxt") String patientIdTxt,
                                         @Param("orgIdTxt") String orgIdTxt);

    @Modifying
    @Query(value = """
        DELETE FROM medical_problems
        WHERE CAST(id         AS TEXT) = :idTxt
          AND CAST(patient_id AS TEXT) = :patientIdTxt
          AND CAST(org_id     AS TEXT) = :orgIdTxt
        """, nativeQuery = true)
    int deleteOneByIdAndPatientIdAndOrgIdText(@Param("idTxt") String idTxt,
                                              @Param("patientIdTxt") String patientIdTxt,
                                              @Param("orgIdTxt") String orgIdTxt);

    @Query(value = """
        SELECT * FROM medical_problems
        WHERE CAST(org_id AS TEXT) = :orgIdTxt
        ORDER BY patient_id, id
        """, nativeQuery = true)
    List<MedicalProblem> findByOrgIdText(@Param("orgIdTxt") String orgIdTxt);
}
