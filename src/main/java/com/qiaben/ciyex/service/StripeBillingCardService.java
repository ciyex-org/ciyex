package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.StripeBillingCardDto;
import com.qiaben.ciyex.entity.StripeBillingCard;
import com.qiaben.ciyex.repository.StripeBillingCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StripeBillingCardService {

    private final StripeBillingCardRepository repository;

    /* ---------------- CREATE ---------------- */
    @Transactional
    public StripeBillingCardDto create(StripeBillingCardDto dto, Long orgId) {
        StripeBillingCard entity = toEntity(dto);
        entity.setOrgId(orgId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        if (dto.isDefault()) {
            clearDefaultForUser(entity.getUserId(), orgId);
            entity.setIsDefault(true);
        }

        return toDto(repository.save(entity));
    }

    /* ---------------- READ ---------------- */
    public List<StripeBillingCardDto> getAll(Long orgId) {
        return repository.findByOrgId(orgId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<StripeBillingCardDto> getAllByUser(Long userId, Long orgId) {
        return repository.findByUserIdAndOrgId(userId, orgId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<StripeBillingCardDto> getById(Long id, Long orgId) {
        return repository.findByIdAndOrgId(id, orgId).map(this::toDto);
    }

    /* ---------------- UPDATE ---------------- */
    @Transactional
    public StripeBillingCardDto update(Long id, StripeBillingCardDto dto, Long orgId) {
        StripeBillingCard entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        entity.setBrand(dto.getBrand());
        entity.setLast4(dto.getLast4());
        entity.setExpMonth(dto.getExpMonth());
        entity.setExpYear(dto.getExpYear());
        entity.setStripePaymentMethodId(dto.getStripePaymentMethodId());
        entity.setStripeCustomerId(dto.getStripeCustomerId());
        entity.setUpdatedAt(LocalDateTime.now());

        if (dto.isDefault()) {
            clearDefaultForUser(entity.getUserId(), orgId);
            entity.setIsDefault(true);
        } else {
            entity.setIsDefault(false);
        }

        return toDto(repository.save(entity));
    }

    /**
     * ✅ Update only the Stripe Customer ID for a card
     */
    @Transactional
    public void updateCustomerId(Long cardId, Long orgId, String customerId) {
        repository.findByIdAndOrgId(cardId, orgId).ifPresent(card -> {
            card.setStripeCustomerId(customerId);
            card.setUpdatedAt(LocalDateTime.now());
            repository.save(card);
        });
    }

    /**
     * ✅ Find all cards without a Stripe Customer ID (per org).
     */
    public List<StripeBillingCardDto> findAllWithoutCustomer(Long orgId) {
        return repository.findByStripeCustomerIdIsNullAndOrgId(orgId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ---------------- DELETE ---------------- */
    @Transactional
    public void delete(Long id, Long orgId) {
        repository.findByIdAndOrgId(id, orgId).ifPresent(repository::delete);
    }

    /* ---------------- SET DEFAULT ---------------- */
    @Transactional
    public StripeBillingCardDto setDefault(Long id, Long orgId) {
        StripeBillingCard card = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        clearDefaultForUser(card.getUserId(), orgId);
        card.setIsDefault(true);
        card.setUpdatedAt(LocalDateTime.now());

        return toDto(repository.save(card));
    }

    private void clearDefaultForUser(Long userId, Long orgId) {
        // Run in a separate transaction to avoid marking the outer transaction rollback-only
        // if an error happens while clearing defaults.
        List<StripeBillingCard> cards = repository.findByUserIdAndOrgId(userId, orgId);
        boolean changed = false;
        for (StripeBillingCard c : cards) {
            if (c.isDefaultCard()) {
                c.setIsDefault(false);
                changed = true;
            }
        }
        if (changed) {
            repository.saveAll(cards);
        }
    }

    /* ---------------- MAPPERS ---------------- */
    private StripeBillingCardDto toDto(StripeBillingCard entity) {
        return StripeBillingCardDto.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .userId(entity.getUserId())
                .stripePaymentMethodId(entity.getStripePaymentMethodId())
                .stripeCustomerId(entity.getStripeCustomerId())
                .brand(entity.getBrand())
                .last4(entity.getLast4())
                .expMonth(entity.getExpMonth())
                .expYear(entity.getExpYear())
                .defaultCard(entity.isDefaultCard()) // ✅ maps correctly to DTO
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StripeBillingCard toEntity(StripeBillingCardDto dto) {
        return StripeBillingCard.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .userId(dto.getUserId())
                .stripePaymentMethodId(dto.getStripePaymentMethodId())
                .stripeCustomerId(dto.getStripeCustomerId())
                .brand(dto.getBrand())
                .last4(dto.getLast4())
                .expMonth(dto.getExpMonth())
                .expYear(dto.getExpYear())
                .isDefault(dto.isDefault()) // ✅ maps from DTO "isDefault" (JsonProperty)
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
