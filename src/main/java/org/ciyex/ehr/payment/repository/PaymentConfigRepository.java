package org.ciyex.ehr.payment.repository;

import org.ciyex.ehr.payment.entity.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, Long> {

    Optional<PaymentConfig> findByOrgAlias(String orgAlias);
}
