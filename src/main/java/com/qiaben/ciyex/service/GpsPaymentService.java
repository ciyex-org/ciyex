package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GpsPaymentDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.GpsPayment;
import com.qiaben.ciyex.repository.GpsPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GpsPaymentService {

    private final GpsPaymentRepository repository;

    public GpsPaymentService(GpsPaymentRepository repository) {
        this.repository = repository;
    }

    private Long requireOrg(String operation) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context required for " + operation);
        }
        return ctx.getOrgId();
    }

    /* READ */
    public List<GpsPaymentDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repository.findByOrgId(orgId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<GpsPaymentDto> getAllByUser(Long userId) {
        Long orgId = requireOrg("getAllByUser");
        return repository.findByOrgIdAndUserId(orgId, userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public Optional<GpsPaymentDto> getById(Long id) {
        Long orgId = requireOrg("getById");
        return repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .map(this::toDto);
    }

    /* CREATE */
    @Transactional
    public GpsPaymentDto create(GpsPaymentDto dto) {
        Long orgId = requireOrg("create");
        dto.setOrgId(orgId);

        GpsPayment entity = GpsPayment.builder()
                .orgId(orgId)
                .userId(dto.getUserId())
                .cardId(dto.getCardId())
                .gpsTransactionId(dto.getGpsTransactionId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toDto(repository.save(entity));
    }

    /* UPDATE */
    @Transactional
    public GpsPaymentDto update(Long id, GpsPaymentDto patch, Long orgId) {
        GpsPayment existing = repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Payment not found or access denied"));

        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());

        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(existing));
    }

    /* DELETE */
    @Transactional
    public void delete(Long id, Long orgId) {
        GpsPayment payment = repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Payment not found or access denied"));
        repository.delete(payment);
    }

    /* Mapper */
    private GpsPaymentDto toDto(GpsPayment entity) {
        GpsPaymentDto dto = new GpsPaymentDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setUserId(entity.getUserId());
        dto.setCardId(entity.getCardId());
        dto.setGpsTransactionId(entity.getGpsTransactionId());
        dto.setAmount(entity.getAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
