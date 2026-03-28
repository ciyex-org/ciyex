package org.ciyex.ehr.payment.repository;

import org.ciyex.ehr.payment.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Page<PaymentTransaction> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    List<PaymentTransaction> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    Optional<PaymentTransaction> findByIdAndOrgAlias(Long id, String orgAlias);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.orgAlias = :org AND t.status = :status")
    long countByStatus(@Param("org") String orgAlias, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(t.amount),0) FROM PaymentTransaction t WHERE t.orgAlias = :org AND t.status = 'completed' AND t.transactionType = 'payment' AND t.createdAt >= :since")
    BigDecimal sumCompletedSince(@Param("org") String orgAlias, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.orgAlias = :org AND t.status = 'completed' AND t.transactionType = 'payment' AND t.createdAt >= :since")
    long countCompletedSince(@Param("org") String orgAlias, @Param("since") LocalDateTime since);

    long countByOrgAlias(String orgAlias);
}
