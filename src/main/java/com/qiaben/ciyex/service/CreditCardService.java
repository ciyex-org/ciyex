package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CreditCardDto;
import com.qiaben.ciyex.entity.CreditCard;
import com.qiaben.ciyex.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository repository;

    /**
     * Create a new credit card
     */
    @Transactional
    public CreditCardDto create(CreditCardDto dto) {
        log.info("Creating credit card for patient ID: {}", dto.getPatientId());

        // Validate card expiration
        validateCardExpiration(dto.getExpiryMonth(), dto.getExpiryYear());

        // Check for duplicate card number
        if (repository.existsByCardNumberAndPatientId(dto.getCardNumber(), dto.getPatientId())) {
            throw new IllegalArgumentException("This card number already exists for the patient");
        }

        // If this is marked as default, unset other default cards
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            unsetDefaultCards(dto.getPatientId());
        }

        // Detect card type if not provided
        if (dto.getCardType() == null || dto.getCardType().isBlank()) {
            dto.setCardType(detectCardType(dto.getCardNumber()));
        }

        // Generate externalId if not provided
        if (dto.getExternalId() == null || dto.getExternalId().trim().isEmpty()) {
            dto.setExternalId(UUID.randomUUID().toString());
        }

        // Generate fhirId if not provided
        if (dto.getFhirId() == null || dto.getFhirId().trim().isEmpty()) {
            dto.setFhirId(UUID.randomUUID().toString());
        }

        CreditCard entity = mapToEntity(dto);
        CreditCard saved = repository.save(entity);

        log.info("Credit card created successfully with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    /**
     * Get a credit card by ID
     */
    @Transactional(readOnly = true)
    public CreditCardDto getById(Long id) {
        log.info("Retrieving credit card with ID: {}", id);
        CreditCard entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found with ID: " + id));
        return mapToDto(entity);
    }

    /**
     * Get all credit cards for a patient
     */
    @Transactional(readOnly = true)
    public List<CreditCardDto> getByPatientId(Long patientId) {
        log.info("Retrieving all credit cards for patient ID: {}", patientId);
        List<CreditCard> cards = repository.findByPatientId(patientId);
        return cards.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Get active credit cards for a patient
     */
    @Transactional(readOnly = true)
    public List<CreditCardDto> getActiveCardsByPatientId(Long patientId) {
        log.info("Retrieving active credit cards for patient ID: {}", patientId);
        List<CreditCard> cards = repository.findByPatientIdAndIsActiveTrue(patientId);
        return cards.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Get default credit card for a patient
     */
    @Transactional(readOnly = true)
    public CreditCardDto getDefaultCard(Long patientId) {
        log.info("Retrieving default credit card for patient ID: {}", patientId);
        CreditCard card = repository.findByPatientIdAndIsDefaultTrue(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No default card found for patient ID: " + patientId));
        return mapToDto(card);
    }

    /**
     * Update a credit card
     */
    @Transactional
    public CreditCardDto update(Long id, CreditCardDto dto) {
        log.info("Updating credit card with ID: {}", id);

        CreditCard existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found with ID: " + id));

        // Validate card expiration
        validateCardExpiration(dto.getExpiryMonth(), dto.getExpiryYear());

        // If this is marked as default, unset other default cards
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            unsetDefaultCards(existing.getPatientId());
        }

        // Update fields
        existing.setCardHolderName(dto.getCardHolderName());
        existing.setExpiryMonth(dto.getExpiryMonth());
        existing.setExpiryYear(dto.getExpiryYear());
        existing.setBillingAddress(dto.getBillingAddress());
        existing.setBillingCity(dto.getBillingCity());
        existing.setBillingState(dto.getBillingState());
        existing.setBillingZip(dto.getBillingZip());
        existing.setBillingCountry(dto.getBillingCountry());
        existing.setIsDefault(dto.getIsDefault());
        existing.setIsActive(dto.getIsActive());

        CreditCard updated = repository.save(existing);
        log.info("Credit card updated successfully with ID: {}", id);
        return mapToDto(updated);
    }

    /**
     * Set a card as default
     */
    @Transactional
    public CreditCardDto setAsDefault(Long id, Long patientId) {
        log.info("Setting credit card ID: {} as default for patient ID: {}", id, patientId);

        CreditCard card = repository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));

        // Unset other default cards
        unsetDefaultCards(patientId);

        // Set this card as default
        card.setIsDefault(true);
        CreditCard updated = repository.save(card);

        log.info("Credit card set as default successfully");
        return mapToDto(updated);
    }

    /**
     * Deactivate a credit card
     */
    @Transactional
    public void deactivate(Long id) {
        log.info("Deactivating credit card with ID: {}", id);

        CreditCard card = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found with ID: " + id));

        card.setIsActive(false);
        card.setIsDefault(false);
        repository.save(card);

        log.info("Credit card deactivated successfully");
    }

    /**
     * Delete a credit card
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting credit card with ID: {}", id);

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Credit card not found with ID: " + id);
        }

        repository.deleteById(id);
        log.info("Credit card deleted successfully");
    }

    // Helper methods

    private void validateCardExpiration(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        LocalDate expiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        if (now.isAfter(expiry)) {
            throw new IllegalArgumentException("Credit card is expired");
        }
    }

    private void unsetDefaultCards(Long patientId) {
        repository.findByPatientIdAndIsDefaultTrue(patientId).ifPresent(card -> {
            card.setIsDefault(false);
            repository.save(card);
        });
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("3")) {
            return "AMEX";
        } else if (cardNumber.startsWith("6")) {
            return "DISCOVER";
        }
        return "UNKNOWN";
    }

    // Mapping methods

    private CreditCard mapToEntity(CreditCardDto dto) {
        return CreditCard.builder()
                .id(dto.getId())
                .externalId(dto.getExternalId())
                .fhirId(dto.getFhirId())
                .patientId(dto.getPatientId())
                .cardHolderName(dto.getCardHolderName())
                .cardNumber(dto.getCardNumber()) // In production, encrypt this
                .cardType(dto.getCardType())
                .expiryMonth(dto.getExpiryMonth())
                .expiryYear(dto.getExpiryYear())
                .cvv(dto.getCvv()) // In production, encrypt this
                .billingAddress(dto.getBillingAddress())
                .billingCity(dto.getBillingCity())
                .billingState(dto.getBillingState())
                .billingZip(dto.getBillingZip())
                .billingCountry(dto.getBillingCountry())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .token(dto.getToken())
                .build();
    }

    private CreditCardDto mapToDto(CreditCard entity) {
        CreditCardDto dto = CreditCardDto.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .fhirId(entity.getFhirId())
                .patientId(entity.getPatientId())
                .cardHolderName(entity.getCardHolderName())
                .cardNumber(entity.getCardNumber()) // Consider returning masked version only
                .maskedCardNumber(entity.getMaskedCardNumber())
                .cardType(entity.getCardType())
                .expiryMonth(entity.getExpiryMonth())
                .expiryYear(entity.getExpiryYear())
                .cvv(entity.getCvv()) // In production, never return CVV
                .billingAddress(entity.getBillingAddress())
                .billingCity(entity.getBillingCity())
                .billingState(entity.getBillingState())
                .billingZip(entity.getBillingZip())
                .billingCountry(entity.getBillingCountry())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .isExpired(entity.isExpired())
                .token(entity.getToken())
                .build();

        // Add audit information
        if (entity.getCreatedDate() != null) {
            CreditCardDto.Audit audit = new CreditCardDto.Audit();
            audit.setCreatedDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null);
            audit.setLastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().toString() : null);
            audit.setCreatedBy(entity.getCreatedBy());
            audit.setUpdatedBy(entity.getLastModifiedBy());
            dto.setAudit(audit);
        }

        return dto;
    }
}
