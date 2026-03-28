package org.ciyex.ehr.portal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.portal.dto.PortalAccessRequestDto;
import org.ciyex.ehr.portal.entity.PortalAccessRequest;
import org.ciyex.ehr.portal.entity.PortalConfig;
import org.ciyex.ehr.portal.repository.PortalAccessRequestRepository;
import org.ciyex.ehr.portal.repository.PortalConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private final PortalConfigRepository configRepo;
    private final PortalAccessRequestRepository requestRepo;
    private final ObjectMapper objectMapper;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ─── Config ───

    @Transactional(readOnly = true)
    public Map<String, Object> getConfig() {
        String org = orgAlias();
        return configRepo.findByOrgAlias(org)
                .or(() -> configRepo.findByOrgAlias("__DEFAULT__"))
                .map(this::parseConfig)
                .orElse(Map.of());
    }

    @Transactional
    public Map<String, Object> saveConfig(Map<String, Object> configMap) {
        String org = orgAlias();
        var entity = configRepo.findByOrgAlias(org)
                .orElse(PortalConfig.builder().orgAlias(org).build());
        try {
            entity.setConfig(objectMapper.writeValueAsString(configMap));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize portal config", e);
        }
        configRepo.save(entity);
        return parseConfig(entity);
    }

    private Map<String, Object> parseConfig(PortalConfig entity) {
        if (entity.getConfig() == null || entity.getConfig().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(entity.getConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse portal config JSON for org={}", entity.getOrgAlias(), e);
            return Map.of();
        }
    }

    // ─── Access Requests ───

    @Transactional(readOnly = true)
    public Page<PortalAccessRequestDto> listRequests(Pageable pageable) {
        return requestRepo.findByOrgAlias(orgAlias(), pageable).map(this::toRequestDto);
    }

    @Transactional(readOnly = true)
    public PortalAccessRequestDto getRequest(Long id) {
        return requestRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .map(this::toRequestDto)
                .orElseThrow(() -> new NoSuchElementException("Access request not found: " + id));
    }

    @Transactional
    public PortalAccessRequestDto approveRequest(Long id, String approvedBy) {
        var request = requestRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Access request not found: " + id));
        request.setStatus("approved");
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        return toRequestDto(requestRepo.save(request));
    }

    @Transactional
    public PortalAccessRequestDto denyRequest(Long id, String reason) {
        var request = requestRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Access request not found: " + id));
        request.setStatus("denied");
        request.setDeniedReason(reason);
        return toRequestDto(requestRepo.save(request));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> requestStats() {
        String org = orgAlias();
        return Map.of(
                "pending", requestRepo.countByOrgAliasAndStatus(org, "pending"),
                "approved", requestRepo.countByOrgAliasAndStatus(org, "approved"),
                "denied", requestRepo.countByOrgAliasAndStatus(org, "denied")
        );
    }

    // ─── Mappers ───

    private PortalAccessRequestDto toRequestDto(PortalAccessRequest e) {
        return PortalAccessRequestDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .dateOfBirth(e.getDateOfBirth() != null ? e.getDateOfBirth().toString() : null)
                .status(e.getStatus())
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
                .deniedReason(e.getDeniedReason())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
