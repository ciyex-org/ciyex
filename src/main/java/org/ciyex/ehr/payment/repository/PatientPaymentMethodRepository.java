package org.ciyex.ehr.payment.repository;

import org.ciyex.ehr.payment.entity.PatientPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PatientPaymentMethodRepository extends JpaRepository<PatientPaymentMethod, Long> {

    List<PatientPaymentMethod> findByOrgAliasAndPatientIdAndIsActiveTrueOrderByCreatedAtDesc(
            String orgAlias, Long patientId);

    Optional<PatientPaymentMethod> findByIdAndOrgAlias(Long id, String orgAlias);

    Optional<PatientPaymentMethod> findByOrgAliasAndPatientIdAndIsDefaultTrue(
            String orgAlias, Long patientId);

    @Modifying
    @Query("UPDATE PatientPaymentMethod m SET m.isDefault = false WHERE m.orgAlias = :org AND m.patientId = :pid AND m.isDefault = true")
    void clearDefaultsForPatient(@Param("org") String orgAlias, @Param("pid") Long patientId);

    long countByOrgAliasAndPatientIdAndIsActiveTrue(String orgAlias, Long patientId);
}
