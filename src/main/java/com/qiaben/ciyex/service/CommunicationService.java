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
import java.util.UUID;

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

        // Validate and map sender
        String senderRef = dto.getSender();
        String senderExternalId = null;
        if (senderRef.startsWith("Provider/")) {
            Long senderId = Long.parseLong(senderRef.replace("Provider/", ""));
            Optional<Provider> providerOpt = providerRepository.findById(senderId);
            if (providerOpt.isEmpty()) {
                log.error("Sender provider not found with ID: {}", senderId);
                throw new RuntimeException("Sender provider not found: " + senderId);
            }
            Provider provider = providerOpt.get();
            if (!currentOrgId.equals(provider.getOrgId())) {
                throw new SecurityException("Sender provider does not belong to current org");
            }
            senderExternalId = provider.getExternalId();
            dto.setSender("Provider/" + senderExternalId);
        } else if (senderRef.startsWith("Patient/")) {
            Long senderId = Long.parseLong(senderRef.replace("Patient/", ""));
            Optional<Patient> patientOpt = patientRepository.findById(senderId);
            if (patientOpt.isEmpty()) {
                log.error("Sender patient not found with ID: {}", senderId);
                throw new RuntimeException("Sender patient not found: " + senderId);
            }
            Patient patient = patientOpt.get();
            if (!currentOrgId.equals(patient.getOrgId())) {
                throw new SecurityException("Sender patient does not belong to current org");
            }
            senderExternalId = patient.getExternalId();
            dto.setSender("Patient/" + senderExternalId);
        } else {
            throw new IllegalArgumentException("Invalid sender reference: " + senderRef);
        }

        // Validate and map recipients
        List<String> mappedRecipients = new ArrayList<>();
        for (String recipientRef : dto.getRecipients()) {
            String recipientExternalId = null;
            if (recipientRef.startsWith("Provider/")) {
                Long recipientId = Long.parseLong(recipientRef.replace("Provider/", ""));
                Optional<Provider> providerOpt = providerRepository.findById(recipientId);
                if (providerOpt.isEmpty()) {
                    log.error("Recipient provider not found with ID: {}", recipientId);
                    throw new RuntimeException("Recipient provider not found: " + recipientId);
                }
                Provider provider = providerOpt.get();
                if (!currentOrgId.equals(provider.getOrgId())) {
                    throw new SecurityException("Recipient provider does not belong to current org");
                }
                recipientExternalId = provider.getExternalId();
                mappedRecipients.add("Provider/" + recipientExternalId);
            } else if (recipientRef.startsWith("Patient/")) {
                Long recipientId = Long.parseLong(recipientRef.replace("Patient/", ""));
                Optional<Patient> patientOpt = patientRepository.findById(recipientId);
                if (patientOpt.isEmpty()) {
                    log.error("Recipient patient not found with ID: {}", recipientId);
                    throw new RuntimeException("Recipient patient not found: " + recipientId);
                }
                Patient patient = patientOpt.get();
                if (!currentOrgId.equals(patient.getOrgId())) {
                    throw new SecurityException("Recipient patient does not belong to current org");
                }
                recipientExternalId = patient.getExternalId();
                mappedRecipients.add("Patient/" + recipientExternalId);
            } else {
                throw new IllegalArgumentException("Invalid recipient reference: " + recipientRef);
            }
        }
        dto.setRecipients(mappedRecipients);

        // Validate and map subject
        if (dto.getSubject() != null && dto.getSubject().startsWith("Patient/")) {
            Long subjectId = Long.parseLong(dto.getSubject().replace("Patient/", ""));
            Optional<Patient> patientOpt = patientRepository.findById(subjectId);
            if (patientOpt.isEmpty()) {
                log.error("Subject patient not found with ID: {}", subjectId);
                throw new RuntimeException("Subject patient not found: " + subjectId);
            }
            Patient patient = patientOpt.get();
            if (!currentOrgId.equals(patient.getOrgId())) {
                throw new SecurityException("Subject patient does not belong to current org");
            }
            String subjectExternalId = patient.getExternalId();
            dto.setSubject("Patient/" + subjectExternalId);
        }

        // Validate inResponseTo with optional UUID check
        if (dto.getInResponseTo() != null) {
            String inResponseToValue = dto.getInResponseTo();
            if (!inResponseToValue.startsWith("Communication/")) {
                throw new IllegalArgumentException("Invalid inResponseTo format: must start with 'Communication/'");
            }
            String inResponseToExternalId = inResponseToValue.replace("Communication/", "");
            try {
                UUID.fromString(inResponseToExternalId); // Validate UUID format
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format for inResponseTo: {}, proceeding without validation", inResponseToExternalId);
                dto.setInResponseTo(null); // Clear invalid inResponseTo
            }

            if (dto.getInResponseTo() != null) {
                Optional<Communication> parentCommOpt = repository.findByExternalIdAndOrgId(inResponseToExternalId, currentOrgId);
                if (parentCommOpt.isEmpty()) {
                    log.warn("Parent communication with externalId {} not found in database, checking FHIR", inResponseToExternalId);
                    ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
                    CommunicationDto parent = externalStorage.get(inResponseToExternalId);
                    if (parent == null) {
                        log.warn("Parent communication not found in FHIR: {}, proceeding without validation", inResponseToExternalId);
                        dto.setInResponseTo(null); // Clear if not found in FHIR
                    }
                }
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
                if (dto.getInResponseTo() == null && communication.getInResponseTo() != null) {
                    dto.setInResponseTo("Communication/" + externalId); // Use new UUID if applicable
                }
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
                Long senderId = Long.parseLong(senderRef.replace("Provider/", ""));
                Optional<Provider> providerOpt = providerRepository.findById(senderId);
                if (providerOpt.isEmpty()) {
                    log.error("Sender provider not found with ID: {}", senderId);
                    throw new RuntimeException("Sender provider not found: " + senderId);
                }
                Provider provider = providerOpt.get();
                if (!currentOrgId.equals(provider.getOrgId())) {
                    throw new SecurityException("Sender provider does not belong to current org");
                }
                dto.setSender("Provider/" + provider.getExternalId());
            } else if (senderRef.startsWith("Patient/")) {
                Long senderId = Long.parseLong(senderRef.replace("Patient/", ""));
                Optional<Patient> patientOpt = patientRepository.findById(senderId);
                if (patientOpt.isEmpty()) {
                    log.error("Sender patient not found with ID: {}", senderId);
                    throw new RuntimeException("Sender patient not found: " + senderId);
                }
                Patient patient = patientOpt.get();
                if (!currentOrgId.equals(patient.getOrgId())) {
                    throw new SecurityException("Sender patient does not belong to current org");
                }
                dto.setSender("Patient/" + patient.getExternalId());
            }
        }

        if (dto.getRecipients() != null) {
            List<String> mappedRecipients = new ArrayList<>();
            for (String recipientRef : dto.getRecipients()) {
                if (recipientRef.startsWith("Provider/")) {
                    Long recipientId = Long.parseLong(recipientRef.replace("Provider/", ""));
                    Optional<Provider> providerOpt = providerRepository.findById(recipientId);
                    if (providerOpt.isEmpty()) {
                        log.error("Recipient provider not found with ID: {}", recipientId);
                        throw new RuntimeException("Recipient provider not found: " + recipientId);
                    }
                    Provider provider = providerOpt.get();
                    if (!currentOrgId.equals(provider.getOrgId())) {
                        throw new SecurityException("Recipient provider does not belong to current org");
                    }
                    mappedRecipients.add("Provider/" + provider.getExternalId());
                } else if (recipientRef.startsWith("Patient/")) {
                    Long recipientId = Long.parseLong(recipientRef.replace("Patient/", ""));
                    Optional<Patient> patientOpt = patientRepository.findById(recipientId);
                    if (patientOpt.isEmpty()) {
                        log.error("Recipient patient not found with ID: {}", recipientId);
                        throw new RuntimeException("Recipient patient not found: " + recipientId);
                    }
                    Patient patient = patientOpt.get();
                    if (!currentOrgId.equals(patient.getOrgId())) {
                        throw new SecurityException("Recipient patient does not belong to current org");
                    }
                    mappedRecipients.add("Patient/" + patient.getExternalId());
                }
            }
            dto.setRecipients(mappedRecipients);
        }

        if (dto.getSubject() != null && dto.getSubject().startsWith("Patient/")) {
            Long subjectId = Long.parseLong(dto.getSubject().replace("Patient/", ""));
            Optional<Patient> patientOpt = patientRepository.findById(subjectId);
            if (patientOpt.isEmpty()) {
                log.error("Subject patient not found with ID: {}", subjectId);
                throw new RuntimeException("Subject patient not found: " + subjectId);
            }
            Patient patient = patientOpt.get();
            if (!currentOrgId.equals(patient.getOrgId())) {
                throw new SecurityException("Subject patient does not belong to current org");
            }
            dto.setSubject("Patient/" + patient.getExternalId());
        }

        if (dto.getInResponseTo() != null) {
            String inResponseToValue = dto.getInResponseTo();
            if (!inResponseToValue.startsWith("Communication/")) {
                throw new IllegalArgumentException("Invalid inResponseTo format: must start with 'Communication/'");
            }
            String inResponseToExternalId = inResponseToValue.replace("Communication/", "");
            try {
                UUID.fromString(inResponseToExternalId); // Validate UUID format
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format for inResponseTo: {}, proceeding without validation", inResponseToExternalId);
                dto.setInResponseTo(null); // Clear invalid inResponseTo
            }

            if (dto.getInResponseTo() != null) {
                Optional<Communication> parentCommOpt = repository.findByExternalIdAndOrgId(inResponseToExternalId, currentOrgId);
                if (parentCommOpt.isEmpty()) {
                    log.warn("Parent communication with externalId {} not found in database, checking FHIR", inResponseToExternalId);
                    ExternalStorage<CommunicationDto> externalStorage = storageResolver.resolve(CommunicationDto.class);
                    CommunicationDto parent = externalStorage.get(inResponseToExternalId);
                    if (parent == null) {
                        log.warn("Parent communication not found in FHIR: {}, proceeding without validation", inResponseToExternalId);
                        dto.setInResponseTo(null); // Clear if not found in FHIR
                    }
                }
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

        // Map sender to internal ID and name with debugging
        if (communication.getSender() != null) {
            if (communication.getSender().startsWith("Provider/")) {
                String externalId = communication.getSender().replace("Provider/", "");
                log.debug("Looking up Provider with externalId: {} and orgId: {}", externalId, communication.getOrgId());
                Optional<Provider> providerOpt = providerRepository.findByExternalIdAndOrgId(communication.getOrgId(), externalId);
                if (providerOpt.isPresent()) {
                    Provider provider = providerOpt.get();
                    dto.setFromId(provider.getId());
                    dto.setFromName(provider.getFirstName() + " " + (provider.getLastName() != null ? provider.getLastName() : ""));
                    log.debug("Found Provider: id={}, name={}", provider.getId(), dto.getFromName());
                } else {
                    log.warn("Provider with externalId {} and orgId {} not found", externalId, communication.getOrgId());
                }
            } else if (communication.getSender().startsWith("Patient/")) {
                String externalId = communication.getSender().replace("Patient/", "");
                log.debug("Looking up Patient with externalId: {} and orgId: {}", externalId, communication.getOrgId());
                Optional<Patient> patientOpt = patientRepository.findByExternalIdAndOrgId(communication.getOrgId(), externalId);
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    dto.setFromId(patient.getId());
                    dto.setFromName(patient.getFirstName() + " " + (patient.getLastName() != null ? patient.getLastName() : ""));
                    log.debug("Found Patient: id={}, name={}", patient.getId(), dto.getFromName());
                } else {
                    log.warn("Patient with externalId {} and orgId {} not found", externalId, communication.getOrgId());
                }
            }
        }

        // Map recipients to internal IDs and names with debugging
        if (communication.getRecipients() != null) {
            List<Long> toIds = new ArrayList<>();
            List<String> toNames = new ArrayList<>();
            for (String recipient : communication.getRecipients().split(",")) {
                recipient = recipient.trim();
                if (recipient.startsWith("Provider/")) {
                    String externalId = recipient.replace("Provider/", "");
                    log.debug("Looking up Provider with externalId: {} and orgId: {}", externalId, communication.getOrgId());
                    Optional<Provider> providerOpt = providerRepository.findByExternalIdAndOrgId(communication.getOrgId(), externalId);
                    if (providerOpt.isPresent()) {
                        Provider provider = providerOpt.get();
                        toIds.add(provider.getId());
                        toNames.add(provider.getFirstName() + " " + (provider.getLastName() != null ? provider.getLastName() : ""));
                        log.debug("Found Provider: id={}, name={}", provider.getId(), toNames.get(toNames.size() - 1));
                    } else {
                        log.warn("Provider with externalId {} and orgId {} not found", externalId, communication.getOrgId());
                    }
                } else if (recipient.startsWith("Patient/")) {
                    String externalId = recipient.replace("Patient/", "");
                    log.debug("Looking up Patient with externalId: {} and orgId: {}", externalId, communication.getOrgId());
                    Optional<Patient> patientOpt = patientRepository.findByExternalIdAndOrgId(communication.getOrgId(), externalId);
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        toIds.add(patient.getId());
                        toNames.add(patient.getFirstName() + " " + (patient.getLastName() != null ? patient.getLastName() : ""));
                        log.debug("Found Patient: id={}, name={}", patient.getId(), toNames.get(toNames.size() - 1));
                    } else {
                        log.warn("Patient with externalId {} and orgId {} not found", externalId, communication.getOrgId());
                    }
                }
            }
            dto.setToIds(toIds);
            dto.setToNames(toNames);
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