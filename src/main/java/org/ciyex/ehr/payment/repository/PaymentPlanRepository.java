package org.ciyex.ehr.payment.repository;

import org.ciyex.ehr.payment.entity.PaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {

    List<PaymentPlan> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    Optional<PaymentPlan> findByIdAndOrgAlias(Long id, String orgAlias);

    List<PaymentPlan> findByOrgAliasAndStatusOrderByNextPaymentDateAsc(String orgAlias, String status);

    long countByOrgAliasAndStatus(String orgAlias, String status);
}
