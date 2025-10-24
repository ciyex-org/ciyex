package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Communication;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.repository.CommunicationRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.service.notification.EmailNotificationService;
import com.qiaben.ciyex.service.notification.SmsNotificationService;
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
    private final ProviderRepository providerRepo;
    private final PatientRepository patientRepo;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;

    public CommunicationService(CommunicationRepository repo,
                                ProviderRepository providerRepo,
                                PatientRepository patientRepo,
                                ExternalStorageResolver storageResolver,
                                OrgIntegrationConfigProvider configProvider,
                                SmsNotificationService smsService,
                                EmailNotificationService emailService) {
        this.repo = repo;
        this.providerRepo = providerRepo;
        this.patientRepo = patientRepo;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    /* ------------------- CREATE ------------------- */
    @Transactional
    public CommunicationDto create(CommunicationDto dto) {
        String now = LocalDateTime.now().toString();

        Long providerId = dto.getProviderId();
        if (providerId == null && dto.getSender() != null && dto.getSender().startsWith("Provider/")) {
            try {
                providerId = Long.valueOf(dto.getSender().split("/")[1]);
            } catch (Exception e) {
                log.warn("Invalid provider sender: {}", dto.getSender());
            }
        }

        Long patientId = dto.getPatientId();
        if (patientId == null && dto.getRecipients() != null) {
            for (String rec : dto.getRecipients()) {
                if (rec.startsWith("Patient/")) {
                    try {
                        patientId = Long.valueOf(rec.split("/")[1]);
                    } catch (Exception e) {
                        log.warn("Invalid patient recipient: {}", rec);
                    }
                }
            }
        }

        Communication entity = Communication.builder()
                .status(dto.getStatus() != null ? dto.getStatus() : CommunicationStatus.SENT)
                .category(dto.getCategory())
                .sentDate(dto.getSentDate() != null ? dto.getSentDate() : now)
                .createdDate(now)
                .lastModifiedDate(now)
                .payload(dto.getPayload())
                .sender(dto.getSender())
                .recipients(dto.getRecipients() != null ? String.join(",", dto.getRecipients()) : null)
                .subject(dto.getSubject())
                .inResponseTo(dto.getInResponseTo())
                .patientId(patientId)
                .providerId(providerId)
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

        // send SMS & Email notifications
        if (saved.getPatientId() != null) {
            patientRepo.findById(saved.getPatientId()).ifPresent(patient -> {
                boolean smsOk = false;
                boolean emailOk = false;
                try {
                    if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isBlank()) {
                        smsService.sendSms(patient.getPhoneNumber(), saved.getPayload());
                        smsOk = true;
                    }
                    if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
                        emailService.sendEmail(
                                patient.getEmail(),
                                saved.getSubject() != null ? saved.getSubject() : "New Message",
                                saved.getPayload()
                        );
                        emailOk = true;
                    }
                } catch (Exception e) {
                    log.error("Notification failed for patient {}: {}", patient.getId(), e.getMessage(), e);
                }
                log.info("Notification sent for patient {} (SMS={}, Email={})",
                        patient.getId(), smsOk, emailOk);
            });
        }

        return toDto(saved);
    }

    /* ------------------- GET ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getByPatientId(Long patientId) {
        return null; /*repo.findAllByPatientId(Collections.singleton(patientId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());*/
    }

    @Transactional(readOnly = true)
    public CommunicationDto getItem(Long patientId, Long id) {
        return null;/*repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId))
                .stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Communication not found: " + id));*/
    }

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public CommunicationDto updateItem(Long patientId, Long id, CommunicationDto patch) {/*
        List<Communication> rows = repo.findAllByPatientIdAndOrgIdText(
                String.valueOf(patientId), String.valueOf(orgId));
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
        return toDto(row);*/
        return null;
    }

    /* ------------------- SET STATUS ------------------- */
    @Transactional
    public CommunicationDto setStatus(Long id, CommunicationStatus status) {
        Communication comm = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        comm.setStatus(status);
        comm.setLastModifiedDate(LocalDateTime.now().toString());
        Communication saved = repo.save(comm);

        if (saved.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.update(toDto(saved), saved.getExternalId());
            }
        }
        return toDto(saved);
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void deleteItem(Long patientId, Long id) {
      /*  List<Communication> rows = repo.findAllByPatientIdAndOrgIdText(
                String.valueOf(patientId), String.valueOf(orgId));
        String externalId = rows.stream().findFirst().map(Communication::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientIdAndOrgIdText(
                String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId));
        if (n == 0) throw new RuntimeException("Delete failed: not found");

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                List<Communication> fresh = repo.findAllByPatientIdAndOrgIdText(
                        String.valueOf(patientId), String.valueOf(orgId));
                if (fresh.isEmpty()) ext.delete(externalId);
                else ext.update(toDto(fresh.get(0)), externalId);
            }
        }*/
    }

    @Transactional
    public void deleteItemById(Long id) {
        Communication row = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Delete failed: not found"));

        String externalId = row.getExternalId();
        repo.delete(row);

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.delete(externalId);
            }
        }
    }

    /* ------------------- SEARCH ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> searchAll() {
        /*return repo.findByOrgIdText(String.valueOf(orgId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());*/
        return null;
    }

    /* ------------------- MAPPER ------------------- */
    private CommunicationDto toDto(Communication r) {
        CommunicationDto dto = new CommunicationDto();
        dto.setId(r.getId());
        dto.setExternalId(r.getExternalId());
        dto.setStatus(r.getStatus());
        dto.setCategory(r.getCategory());
        dto.setSentDate(r.getSentDate());
        dto.setCreatedDate(r.getCreatedDate());
        dto.setLastModifiedDate(r.getLastModifiedDate());
        dto.setPayload(r.getPayload());
        dto.setSender(r.getSender());
        dto.setRecipients(r.getRecipients() != null ? Arrays.asList(r.getRecipients().split(",")) : Collections.emptyList());
        dto.setSubject(r.getSubject());
        dto.setInResponseTo(r.getInResponseTo());
        dto.setPatientId(r.getPatientId());
        dto.setProviderId(r.getProviderId());

        // From (provider name)
        if (r.getProviderId() != null) {
            providerRepo.findById(r.getProviderId())
                    .ifPresent(p -> dto.setFromName(p.getFirstName() + " " + p.getLastName()));
        } else if (r.getSender() != null && r.getSender().startsWith("Provider/")) {
            try {
                Long pid = Long.valueOf(r.getSender().split("/")[1]);
                providerRepo.findById(pid)
                        .ifPresent(p -> dto.setFromName(p.getFirstName() + " " + p.getLastName()));
            } catch (Exception e) {
                log.warn("Could not parse provider from sender={}", r.getSender());
            }
        }
        if (dto.getFromName() == null) dto.setFromName("Unknown Provider");

        // To (patient names)
        List<String> toNames = new ArrayList<>();
        if (r.getPatientId() != null) {
            patientRepo.findById(r.getPatientId())
                    .ifPresent(pt -> toNames.add(pt.getFirstName() + " " + pt.getLastName()));
        } else if (r.getRecipients() != null) {
            for (String rec : r.getRecipients().split(",")) {
                if (rec.startsWith("Patient/")) {
                    try {
                        Long pid = Long.valueOf(rec.split("/")[1]);
                        patientRepo.findById(pid)
                                .ifPresent(pt -> toNames.add(pt.getFirstName() + " " + pt.getLastName()));
                    } catch (Exception e) {
                        log.warn("Could not parse patient from recipient={}", rec);
                    }
                }
            }
        }
        dto.setToNames(toNames);

        return dto;
    }
}
