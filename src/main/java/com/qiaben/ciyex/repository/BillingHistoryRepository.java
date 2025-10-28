package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.BillingHistory;
import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingHistoryRepository extends JpaRepository<BillingHistory, Long> {

    /**
     * Find all billing history records for a given org + user.
     */
    List<BillingHistory> findByOrgIdAndUserIdOrderByCreatedAtDesc(Long orgId, Long userId);

    /**
     * Find all billing history records for an org.
     */
    List<BillingHistory> findByOrgIdOrderByCreatedAtDesc(Long orgId);

    /**
     * Find Stripe billing record by PaymentIntent ID.
     */
    Optional<BillingHistory> findByStripePaymentIntentIdAndProvider(
            String stripePaymentIntentId,
            BillingProvider provider
    );

    /**
     * Find GPS billing record by Transaction ID.
     */
    Optional<BillingHistory> findByGpsTransactionIdAndProvider(
            String gpsTransactionId,
            BillingProvider provider
    );

    /**
     * Find all billing records by provider (e.g. STRIPE, GPS).
     */
    List<BillingHistory> findByOrgIdAndProviderOrderByCreatedAtDesc(Long orgId, BillingProvider provider);

    /**
     * Find billing record by linked invoice.
     */
    Optional<BillingHistory> findByInvoiceBill_Id(Long invoiceBillId);

    /**
     * Directly update status for Stripe payment (optional, for webhook optimization).
     */
    @Modifying
    @Query("UPDATE BillingHistory b SET b.status = :status, b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.stripePaymentIntentId = :paymentIntentId AND b.provider = com.qiaben.ciyex.entity.BillingHistory$BillingProvider.STRIPE")
    int updateStripeStatus(String paymentIntentId, BillingStatus status);

    /**
     * Directly update status for GPS payment (optional, for webhook optimization).
     */
    @Modifying
    @Query("UPDATE BillingHistory b SET b.status = :status, b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.gpsTransactionId = :transactionId AND b.provider = com.qiaben.ciyex.entity.BillingHistory$BillingProvider.GPS")
    int updateGpsStatus(String transactionId, BillingStatus status);
}
