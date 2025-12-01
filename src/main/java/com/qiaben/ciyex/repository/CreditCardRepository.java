package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    /**
     * Find all credit cards for a specific patient
     */
    List<CreditCard> findByPatientId(Long patientId);

    /**
     * Find all active credit cards for a specific patient
     */
    List<CreditCard> findByPatientIdAndIsActiveTrue(Long patientId);

    /**
     * Find the default credit card for a patient
     */
    Optional<CreditCard> findByPatientIdAndIsDefaultTrue(Long patientId);

    /**
     * Find a specific credit card by patient ID and card ID
     */
    @Query("SELECT c FROM CreditCard c WHERE c.id = :cardId AND c.patientId = :patientId")
    Optional<CreditCard> findByIdAndPatientId(@Param("cardId") Long cardId, @Param("patientId") Long patientId);

    /**
     * Check if a card number already exists for a patient
     */
    @Query("SELECT COUNT(c) > 0 FROM CreditCard c WHERE c.cardNumber = :cardNumber AND c.patientId = :patientId")
    boolean existsByCardNumberAndPatientId(@Param("cardNumber") String cardNumber, @Param("patientId") Long patientId);

    /**
     * Count active credit cards for a patient
     */
    @Query("SELECT COUNT(c) FROM CreditCard c WHERE c.patientId = :patientId AND c.isActive = true")
    long countActiveCardsByPatientId(@Param("patientId") Long patientId);

    /**
     * Find all expired cards
     */
    @Query("SELECT c FROM CreditCard c WHERE " +
           "(c.expiryYear < YEAR(CURRENT_DATE)) OR " +
           "(c.expiryYear = YEAR(CURRENT_DATE) AND c.expiryMonth < MONTH(CURRENT_DATE))")
    List<CreditCard> findExpiredCards();
}
