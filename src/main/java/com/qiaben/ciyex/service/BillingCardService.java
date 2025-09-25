package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.BillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.BillingCard;
import com.qiaben.ciyex.repository.BillingCardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BillingCardService {

    private final BillingCardRepository repo;

    @PersistenceContext
    private EntityManager em; // ✅ Used to check user existence in public.users

    public BillingCardService(BillingCardRepository repo) {
        this.repo = repo;
    }

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    /* ------------------- CREATE ------------------- */
    @Transactional
    public BillingCardDto create(BillingCardDto dto) {
        Long orgId = requireOrg("create");
        dto.setOrgId(orgId);

        if (dto.getStripePaymentMethodId() == null || dto.getStripePaymentMethodId().isBlank()) {
            throw new IllegalArgumentException("stripePaymentMethodId is required to save a billing card");
        }

        // ✅ Verify user exists in public.users
        Long userCount = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM public.users WHERE id = :id")
                .setParameter("id", dto.getUserId())
                .getSingleResult()).longValue();
        if (userCount == 0) {
            throw new IllegalArgumentException("User not found in public.users: " + dto.getUserId());
        }

        BillingCard entity = BillingCard.builder()
                .orgId(orgId)
                .userId(dto.getUserId())
                .stripePaymentMethodId(dto.getStripePaymentMethodId())
                .stripeCustomerId(dto.getStripeCustomerId())
                .brand(dto.getBrand())
                .last4(dto.getLast4())
                .expMonth(dto.getExpMonth())
                .expYear(dto.getExpYear())
                .isDefault(Boolean.TRUE.equals(dto.getIsDefault()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // If this is the first card for user/org, make it default automatically
        if (repo.findByUserIdAndOrgIdOrderByCreatedAtDesc(dto.getUserId(), orgId).isEmpty()) {
            entity.setIsDefault(true);
        }

        BillingCard saved = repo.save(entity);
        return toDto(saved);
    }

    /* ------------------- GET ONE ------------------- */
    @Transactional(readOnly = true)
    public Optional<BillingCardDto> getByUser(Long userId, Long orgId) {
        return repo.findByUserIdAndOrgId(userId, orgId).map(this::toDto);
    }

    /* ------------------- GET ALL CARDS FOR USER ------------------- */
    @Transactional(readOnly = true)
    public List<BillingCardDto> getAllByUser(Long userId, Long orgId) {
        // ✅ Optionally verify existence in public.users
        Long userCount = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM public.users WHERE id = :id")
                .setParameter("id", userId)
                .getSingleResult()).longValue();
        if (userCount == 0) {
            throw new IllegalArgumentException("User not found in public.users: " + userId);
        }

        return repo.findByUserIdAndOrgIdOrderByCreatedAtDesc(userId, orgId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ------------------- LIST ALL (org-wide) ------------------- */
    @Transactional(readOnly = true)
    public List<BillingCardDto> searchAll(Long orgId) {
        return repo.findByOrgId(orgId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public BillingCardDto update(Long id, BillingCardDto patch, Long orgId) {
        BillingCard row = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("Card not found id=" + id));

        if (patch.getBrand() != null) row.setBrand(patch.getBrand());
        if (patch.getLast4() != null) row.setLast4(patch.getLast4());
        if (patch.getExpMonth() != null) row.setExpMonth(patch.getExpMonth());
        if (patch.getExpYear() != null) row.setExpYear(patch.getExpYear());
        row.setUpdatedAt(LocalDateTime.now());

        return toDto(repo.save(row));
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void delete(Long id, Long orgId) {
        BillingCard row = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("Delete failed: not found"));

        boolean wasDefault = Boolean.TRUE.equals(row.getIsDefault());
        repo.delete(row);

        if (wasDefault) {
            repo.findFirstByOrgIdOrderByCreatedAtAsc(orgId).ifPresent(next -> {
                next.setIsDefault(true);
                next.setUpdatedAt(LocalDateTime.now());
                repo.save(next);
            });
        }
    }

    /* ------------------- SET DEFAULT ------------------- */
    @Transactional
    public BillingCardDto setDefault(Long id, Long orgId) {
        List<BillingCard> all = repo.findByOrgId(orgId);
        for (BillingCard c : all) {
            if (Boolean.TRUE.equals(c.getIsDefault())) {
                c.setIsDefault(false);
                c.setUpdatedAt(LocalDateTime.now());
                repo.save(c);
            }
        }

        BillingCard chosen = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("Card not found id=" + id));

        chosen.setIsDefault(true);
        chosen.setUpdatedAt(LocalDateTime.now());
        BillingCard updated = repo.save(chosen);

        return toDto(updated);
    }

    /* ------------------- MAPPER ------------------- */
    private BillingCardDto toDto(BillingCard r) {
        return BillingCardDto.builder()
                .id(r.getId())
                .orgId(r.getOrgId())
                .userId(r.getUserId())
                .stripePaymentMethodId(r.getStripePaymentMethodId())
                .stripeCustomerId(r.getStripeCustomerId())
                .brand(r.getBrand())
                .last4(r.getLast4())
                .expMonth(r.getExpMonth())
                .expYear(r.getExpYear())
                .isDefault(r.getIsDefault())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
