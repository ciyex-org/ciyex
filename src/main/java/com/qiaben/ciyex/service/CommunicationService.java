package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Communication;
import com.qiaben.ciyex.repository.CommunicationRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommunicationService {

    private final CommunicationRepository repo;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public CommunicationService(CommunicationRepository repo,
                                ExternalStorageResolver storageResolver,
                                OrgIntegrationConfigProvider configProvider) {
        this.repo = repo;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    @Transactional
    public CommunicationDto create(CommunicationDto dto) {
        Long orgId = requireOrg("create");
        dto.setOrgId(orgId);
        String now = LocalDateTime.now().toString();

        Communication entity = Communication.builder()
                .orgId(orgId)
                .status(dto.getStatus())
                .category(dto.getCategory())
                .sentDate(dto.getSentDate())
                .createdDate(now)
                .lastModifiedDate(now)
                .payload(dto.getPayload())
                .sender(dto.getSender())
                .recipients(String.join(",", dto.getRecipients()))
                .subject(dto.getSubject())
                .inResponseTo(dto.getInResponseTo())
                .patientId(dto.getPatientId())
                .providerId(dto.getProviderId())
                .build();

        Communication saved = repo.save(entity);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
            CommunicationDto snap = toDto(saved);
            String externalId = ext.create(snap);
            saved.setExternalId(externalId);
            repo.save(saved);
        }
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommunicationDto> getByPatientId(Long patientId) {
        Long orgId = requireOrg("getByPatientId");
        return repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommunicationDto getItem(Long patientId, Long id) {
        Long orgId = requireOrg("getItem");
        return repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId))
                .stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Communication not found: " + id));
    }

    @Transactional
    public CommunicationDto updateItem(Long patientId, Long id, CommunicationDto patch) {
        Long orgId = requireOrg("updateItem");
        List<Communication> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        Communication row = rows.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        if (patch.getPayload() != null) row.setPayload(patch.getPayload());
        if (patch.getStatus() != null) row.setStatus(patch.getStatus());
        row.setLastModifiedDate(LocalDateTime.now().toString());
        repo.save(row);

        if (row.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.update(toDto(row), row.getExternalId());
            }
        }
        return toDto(row);
    }

    @Transactional
    public void deleteItem(Long patientId, Long id) {
        Long orgId = requireOrg("deleteItem");
        List<Communication> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        String externalId = rows.stream().findFirst().map(Communication::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientIdAndOrgIdText(
                String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId));
        if (n == 0) throw new RuntimeException("Delete failed: not found");

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                List<Communication> fresh = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
                if (fresh.isEmpty()) ext.delete(externalId);
                else ext.update(toDto(fresh.get(0)), externalId);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CommunicationDto> searchAll() {
        Long orgId = requireOrg("searchAll");
        return repo.findByOrgIdText(String.valueOf(orgId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CommunicationDto toDto(Communication r) {
        CommunicationDto dto = new CommunicationDto();
        dto.setId(r.getId());
        dto.setExternalId(r.getExternalId());
        dto.setOrgId(r.getOrgId());
        dto.setStatus(r.getStatus());
        dto.setCategory(r.getCategory());
        dto.setSentDate(r.getSentDate());
        dto.setCreatedDate(r.getCreatedDate());
        dto.setLastModifiedDate(r.getLastModifiedDate());
        dto.setPayload(r.getPayload());
        dto.setSender(r.getSender());
        dto.setRecipients(Arrays.asList(r.getRecipients().split(",")));
        dto.setSubject(r.getSubject());
        dto.setInResponseTo(r.getInResponseTo());
        dto.setPatientId(r.getPatientId());
        dto.setProviderId(r.getProviderId());
        return dto;
    }
}