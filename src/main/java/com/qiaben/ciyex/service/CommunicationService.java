package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.entity.Communication;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.repository.CommunicationRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CommunicationService {

    private final CommunicationRepository repository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public CommunicationService(CommunicationRepository repository, PatientRepository patientRepository,
                                ProviderRepository providerRepository, ExternalStorageResolver storageResolver,
                                OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional(readOnly = true)
    public long countCommunicationsForCurrentOrg() {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            log.error("No orgId found in RequestContext during count");
            throw new SecurityException("No orgId available in request context");
        }
        log.info("Counting communications for orgId: {}", orgId);
        return repository.countByOrgId(orgId);
    }

    @Transactional
    public CommunicationDto create(CommunicationDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during create");
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to create new communication", currentOrgId);
        dto.setOrgId(currentOrgId);

        if (dto.getStatus() == null || dto.getSender() == null || (dto.getRecipients() == null || dto.getRecipients().isEmpty())) {
            throw new IllegalArgumentException("Status, sender, and at least one recipient are required");
        }

        String senderRef = dto.getSender();
        if (senderRef.startsWith("Provider/")) {
            String senderExternalId = senderRef.replace("Provider/", "");
            Provider provider = providerRepository.findAllByOrgId(currentOrgId).stream()
                    .filter(p -> p.getExternalId().equals(senderExternalId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sender provider not found: " + senderExternalId));
        } else if (senderRef.startsWith("Patient/")) {
            String senderExternalId = senderRef.replace("Patient/", "");
            Patient patient = patientRepository.findAllByOrgId(currentOrgId).stream()
                    .filter(p -> p.getExternalId().equals(senderExternalId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sender patient not found: " + senderExternalId));
        } else {
            throw new IllegalArgumentException("Invalid sender reference: " + senderRef);
        }

        for (String recipientRef : dto.getRecipients()) {
            if (recipientRef.startsWith("Provider/")) {
                String recipientExternalId = recipientRef.replace("Provider/", "");
                providerRepository.findAllByOrgId(currentOrgId).stream()
                        .filter(p -> p.getExternalId().equals(recipientExternalId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Recipient provider not found: " + recipientExternalId));
            } else if (recipientRef.startsWith("Patient/")) {
                String recipientExternalId = recipientRef.replace("Patient/", "");
                patientRepository.findAllByOrgId(currentOrgId).stream()
                        .filter(p -> p.getExternalId().equals(recipientExternalId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Recipient patient not found: " + recipientExternalId));
            } else {
                throw new IllegalArgumentException("Invalid recipient reference: " + recipientRef);
            }
        }

        if (dto.getSubject() != null && dto.getSubject().startsWith("Patient/")) {
            String subjectExternalId = dto.getSubject().replace("Patient/", "");
            patientRepository.findAllByOrgId(currentOrgId).stream()
                    .filter(p -> p.getExternalId().equals(subjectExternalId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Subject patient not found: " + subjectExternalId));
        }

        if (dto.getInResponseTo() != null) {
            String inResponseToExternalId = dto.getInResponseTo().replace("Communication/", "");
            ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
            CommunicationDto parent = externalStorage.get(inResponseToExternalId);
            if (parent == null) {
                throw new RuntimeException("Parent communication not found: " + inResponseToExternalId);
            }
        }

        Communication communication = mapToEntity(dto);
        communication.setOrgId(currentOrgId);
        communication.setCreatedDate(LocalDateTime.now().toString());
        communication.setLastModifiedDate(LocalDateTime.now().toString());
        String externalId = null;

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created communication in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
            } catch (Exception e) {
                log.error("Failed to create communication in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        communication.setExternalId(externalId);
        communication = repository.save(communication);
        log.debug("Saved communication to DB: id={}, externalId={}, orgId={}", communication.getId(), communication.getExternalId(), communication.getOrgId());
        if (communication.getId() == null) {
            log.error("Database save failed to generate id for communication with externalId: {} and orgId: {}", externalId, currentOrgId);
            throw new RuntimeException("Failed to generate id for new communication");
        }
        dto.setId(communication.getId());
        dto.setExternalId(externalId);
        log.info("Created communication with id: {} and externalId: {} in DB for orgId: {}", communication.getId(), externalId, currentOrgId);

        return dto;
    }

    @Transactional(readOnly = true)
    public CommunicationDto getById(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during getById for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to communication with id: {}", currentOrgId, id);

        Communication communication = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found with id: " + id));
        log.debug("Fetched communication from DB: id={}, externalId={}, orgId={}", communication.getId(), communication.getExternalId(), communication.getOrgId());
        if (!currentOrgId.equals(communication.getOrgId())) {
            throw new SecurityException("Access denied: Communication id " + id + " does not belong to orgId " + currentOrgId);
        }

        CommunicationDto communicationDto = mapToDto(communication);
        if (communication.getExternalId() != null) {
            CommunicationDto fhirCommunicationDto = getCommunicationFromFhir(communication.getExternalId());
            if (fhirCommunicationDto != null) {
                log.info("Successfully fetched extended data from FHIR for communication id: {}", id);
                communicationDto.setPayload(fhirCommunicationDto.getPayload());
            }
        }

        return communicationDto;
    }

    public CommunicationDto getCommunicationFromFhir(String externalId) {
        log.debug("Fetching extended communication data from FHIR with externalId: {}", externalId);
        ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
        return externalStorage.get(externalId);
    }

    @Transactional
    public CommunicationDto update(Long id, CommunicationDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during update for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to communication with id: {}", currentOrgId, id);

        Communication communication = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found with id: " + id));
        log.debug("Fetched communication from DB: id={}, externalId={}, orgId={}", communication.getId(), communication.getExternalId(), communication.getOrgId());
        if (!currentOrgId.equals(communication.getOrgId())) {
            throw new SecurityException("Access denied: Communication id " + id + " does not belong to orgId " + currentOrgId);
        }

        if (dto.getSender() != null) {
            String senderRef = dto.getSender();
            if (senderRef.startsWith("Provider/")) {
                String senderExternalId = senderRef.replace("Provider/", "");
                providerRepository.findAllByOrgId(currentOrgId).stream()
                        .filter(p -> p.getExternalId().equals(senderExternalId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Sender provider not found: " + senderExternalId));
            } else if (senderRef.startsWith("Patient/")) {
                String senderExternalId = senderRef.replace("Patient/", "");
                patientRepository.findAllByOrgId(currentOrgId).stream()
                        .filter(p -> p.getExternalId().equals(senderExternalId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Sender patient not found: " + senderExternalId));
            }
        }

        if (dto.getRecipients() != null) {
            for (String recipientRef : dto.getRecipients()) {
                if (recipientRef.startsWith("Provider/")) {
                    String recipientExternalId = recipientRef.replace("Provider/", "");
                    providerRepository.findAllByOrgId(currentOrgId).stream()
                            .filter(p -> p.getExternalId().equals(recipientExternalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Recipient provider not found: " + recipientExternalId));
                } else if (recipientRef.startsWith("Patient/")) {
                    String recipientExternalId = recipientRef.replace("Patient/", "");
                    patientRepository.findAllByOrgId(currentOrgId).stream()
                            .filter(p -> p.getExternalId().equals(recipientExternalId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Recipient patient not found: " + recipientExternalId));
                }
            }
        }

        if (dto.getSubject() != null && dto.getSubject().startsWith("Patient/")) {
            String subjectExternalId = dto.getSubject().replace("Patient/", "");
            patientRepository.findAllByOrgId(currentOrgId).stream()
                    .filter(p -> p.getExternalId().equals(subjectExternalId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Subject patient not found: " + subjectExternalId));
        }

        if (dto.getInResponseTo() != null) {
            String inResponseToExternalId = dto.getInResponseTo().replace("Communication/", "");
            ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
            CommunicationDto parent = externalStorage.get(inResponseToExternalId);
            if (parent == null) {
                throw new RuntimeException("Parent communication not found: " + inResponseToExternalId);
            }
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && communication.getExternalId() != null) {
            try {
                ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
                externalStorage.update(dto, communication.getExternalId());
                log.info("Successfully updated communication with id: {} and externalId: {} in external storage for orgId: {}", id, communication.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to update communication in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        updateEntityFromDto(communication, dto);
        communication.setLastModifiedDate(LocalDateTime.now().toString());
        communication = repository.save(communication);
        dto.setId(communication.getId());
        dto.setExternalId(communication.getExternalId());
        log.info("Updated communication with id: {} and externalId: {} in DB for orgId: {}", id, communication.getExternalId(), currentOrgId);

        return dto;
    }

    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during delete for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to communication with id: {}", currentOrgId, id);

        Communication communication = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found with id: " + id));
        if (!currentOrgId.equals(communication.getOrgId())) {
            throw new SecurityException("Access denied: Communication id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && communication.getExternalId() != null) {
            try {
                ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
                externalStorage.delete(communication.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete communication from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(communication);
        log.info("Deleted communication with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<CommunicationDto>> getAllCommunications(Pageable pageable) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            return ApiResponse.<Page<CommunicationDto>>builder()
                    .success(false)
                    .message("No orgId available in request context")
                    .build();
        }

        Page<Communication> commPage = repository.findByOrgId(currentOrgId, pageable);
        Page<CommunicationDto> dtoPage = commPage.map(this::mapToDto);

        return ApiResponse.<Page<CommunicationDto>>builder()
                .success(true)
                .message("Communications retrieved successfully")
                .data(dtoPage)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CommunicationDto> getCommunicationsForPatient(Long patientId, Pageable pageable) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext");
            throw new SecurityException("No orgId available in request context");
        }
        if (patientId == null) {
            log.error("No patientId provided in request body");
            throw new IllegalArgumentException("Patient ID is required");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));
        if (!currentOrgId.equals(patient.getOrgId())) {
            throw new SecurityException("Access denied: Patient does not belong to current org");
        }
        String ref = "Patient/" + patient.getExternalId();

        Page<Communication> commPage = repository.findCommunicationsForPatientRef(currentOrgId, ref, pageable);
        Page<CommunicationDto> dtoPage = commPage.map(this::mapToDto);
        return dtoPage;
    }

    @Transactional(readOnly = true)
    public Page<CommunicationDto> getCommunicationsForProvider(Long providerId, Pageable pageable) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext");
            throw new SecurityException("No orgId available in request context");
        }
        if (providerId == null) {
            log.error("No providerId provided in request body");
            throw new IllegalArgumentException("Provider ID is required");
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + providerId));
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider does not belong to current org");
        }
        String ref = "Provider/" + provider.getExternalId();

        Page<Communication> commPage = repository.findCommunicationsForProviderRef(currentOrgId, ref, pageable);
        Page<CommunicationDto> dtoPage = commPage.map(this::mapToDto);
        return dtoPage;
    }

    @Transactional(readOnly = true)
    public List<CommunicationDto> getThread(String externalId) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext");
            throw new SecurityException("No orgId available in request context");
        }

        List<CommunicationDto> thread = new ArrayList<>();
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        ExternalStorage<CommunicationDto> externalStorage = null;
        if (storageType != null) {
            externalStorage = storageResolver.resolve(CommunicationDto.class);
        }

        Optional<Communication> optCurrent = repository.findByExternalIdAndOrgId(externalId, currentOrgId);
        if (optCurrent.isEmpty()) {
            return thread;
        }
        Communication currentComm = optCurrent.get();
        CommunicationDto currentDto = mapToDto(currentComm);
        if (storageType != null) {
            CommunicationDto ext = externalStorage.get(externalId);
            if (ext != null) {
                currentDto.setPayload(ext.getPayload());
            }
        }
        thread.add(currentDto);

        String parentRef = currentComm.getInResponseTo();
        while (parentRef != null) {
            String parentExternalId = parentRef.replace("Communication/", "");
            Optional<Communication> optParent = repository.findByExternalIdAndOrgId(parentExternalId, currentOrgId);
            if (optParent.isEmpty()) {
                break;
            }
            Communication parentComm = optParent.get();
            CommunicationDto parentDto = mapToDto(parentComm);
            if (storageType != null) {
                CommunicationDto ext = externalStorage.get(parentExternalId);
                if (ext != null) {
                    parentDto.setPayload(ext.getPayload());
                }
            }
            thread.add(0, parentDto);
            parentRef = parentComm.getInResponseTo();
        }

        String childRef = "Communication/" + externalId;
        List<Communication> childrenComms = repository.findByInResponseToAndOrgId(childRef, currentOrgId);
        List<CommunicationDto> childrenDtos = new ArrayList<>();
        for (Communication childComm : childrenComms) {
            CommunicationDto childDto = mapToDto(childComm);
            if (storageType != null && childComm.getExternalId() != null) {
                CommunicationDto ext = externalStorage.get(childComm.getExternalId());
                if (ext != null) {
                    childDto.setPayload(ext.getPayload());
                }
            }
            childrenDtos.add(childDto);
        }
        childrenDtos.sort(Comparator.comparing(CommunicationDto::getSentDate).reversed());
        thread.addAll(childrenDtos);

        return thread;
    }

    private Communication mapToEntity(CommunicationDto dto) {
        Communication communication = new Communication();
        communication.setStatus(dto.getStatus());
        communication.setCategory(dto.getCategory());
        communication.setSentDate(dto.getSentDate());
        communication.setSender(dto.getSender());
        communication.setRecipients(dto.getRecipients() != null ? String.join(",", dto.getRecipients()) : null);
        communication.setSubject(dto.getSubject());
        communication.setInResponseTo(dto.getInResponseTo());
        return communication;
    }

    private CommunicationDto mapToDto(Communication communication) {
        CommunicationDto dto = new CommunicationDto();
        dto.setId(communication.getId());
        dto.setExternalId(communication.getExternalId());
        dto.setStatus(communication.getStatus());
        dto.setCategory(communication.getCategory());
        dto.setSentDate(communication.getSentDate());
        dto.setOrgId(communication.getOrgId());
        dto.setSender(communication.getSender());
        dto.setRecipients(communication.getRecipients() != null ? Arrays.asList(communication.getRecipients().split(",")) : null);
        dto.setSubject(communication.getSubject());
        dto.setInResponseTo(communication.getInResponseTo());
        if (communication.getCreatedDate() != null || communication.getLastModifiedDate() != null) {
            CommunicationDto.Audit audit = new CommunicationDto.Audit();
            audit.setCreatedDate(communication.getCreatedDate());
            audit.setLastModifiedDate(communication.getLastModifiedDate());
            dto.setAudit(audit);
        }
        return dto;
    }

    private void updateEntityFromDto(Communication communication, CommunicationDto dto) {
        if (dto.getStatus() != null) communication.setStatus(dto.getStatus());
        if (dto.getCategory() != null) communication.setCategory(dto.getCategory());
        if (dto.getSentDate() != null) communication.setSentDate(dto.getSentDate());
        if (dto.getSender() != null) communication.setSender(dto.getSender());
        if (dto.getRecipients() != null) communication.setRecipients(String.join(",", dto.getRecipients()));
        if (dto.getSubject() != null) communication.setSubject(dto.getSubject());
        if (dto.getInResponseTo() != null) communication.setInResponseTo(dto.getInResponseTo());
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext");
        }
        return orgId;
    }
}