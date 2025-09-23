package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.BillingAutoPayDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.BillingAutoPay;
import com.qiaben.ciyex.repository.BillingAutoPayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BillingAutoPayService {

    private final BillingAutoPayRepository repo;

    public BillingAutoPayService(BillingAutoPayRepository repo) {
        this.repo = repo;
    }

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    /* ------------------- SAVE / CREATE ------------------- */
    @Transactional
    public BillingAutoPayDto save(BillingAutoPayDto dto) {
        Long orgId = requireOrg("save");
        dto.setOrgId(orgId);

        BillingAutoPay entity = BillingAutoPay.builder()
                .orgId(orgId)
                .userId(dto.getUserId())
                .enabled(dto.getEnabled())
                .startDate(dto.getStartDate())
                .frequency(dto.getFrequency())
                .maxAmount(dto.getMaxAmount())
                .cardId(dto.getCardId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BillingAutoPay saved = repo.save(entity);
        return toDto(saved);
    }

    /* ------------------- GET BY USER ------------------- */
    @Transactional(readOnly = true)
    public Optional<BillingAutoPayDto> getByUser(Long userId) {
        Long orgId = requireOrg("getByUser");
        return repo.findByUserIdAndOrgId(userId, orgId).map(this::toDto);
    }

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public BillingAutoPayDto update(Long id, BillingAutoPayDto patch) {
        Long orgId = requireOrg("update");
        BillingAutoPay row = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("AutoPay not found id=" + id));

        if (patch.getEnabled() != null) row.setEnabled(patch.getEnabled());
        if (patch.getStartDate() != null) row.setStartDate(patch.getStartDate());
        if (patch.getFrequency() != null) row.setFrequency(patch.getFrequency());
        if (patch.getMaxAmount() != null) row.setMaxAmount(patch.getMaxAmount());
        if (patch.getCardId() != null) row.setCardId(patch.getCardId());
        row.setUpdatedAt(LocalDateTime.now());

        return toDto(repo.save(row));
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void delete(Long id) {
        Long orgId = requireOrg("delete");
        BillingAutoPay row = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("Delete failed: not found"));
        repo.delete(row);
    }

    /* ------------------- MAPPER ------------------- */
    private BillingAutoPayDto toDto(BillingAutoPay r) {
        return BillingAutoPayDto.builder()
                .id(r.getId())
                .orgId(r.getOrgId())
                .userId(r.getUserId())
                .enabled(r.getEnabled())
                .startDate(r.getStartDate())
                .frequency(r.getFrequency())
                .maxAmount(r.getMaxAmount())
                .cardId(r.getCardId())
                .build();
    }
}
