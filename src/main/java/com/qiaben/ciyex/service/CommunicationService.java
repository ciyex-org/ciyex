package com.qiaben.ciyex.service;

import com.qiaben.ciyex.controller.WebSocketMessagingController;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Communication;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.repository.CommunicationRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.service.notification.EmailNotificationService;
import com.qiaben.ciyex.service.notification.SmsNotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;
    private final Optional<WebSocketMessagingController> webSocketController;
    private final MessageAttachmentService messageAttachmentService;

    @PersistenceContext
    private EntityManager entityManager;

    public CommunicationService(CommunicationRepository repo,
                                ProviderRepository providerRepo,
                                PatientRepository patientRepo,
                                SmsNotificationService smsService,
                                EmailNotificationService emailService,
                                Optional<WebSocketMessagingController> webSocketController,
                                MessageAttachmentService messageAttachmentService) {
        this.repo = repo;
        this.providerRepo = providerRepo;
        this.patientRepo = patientRepo;
        this.smsService = smsService;
        this.emailService = emailService;
        this.webSocketController = webSocketController;
        this.messageAttachmentService = messageAttachmentService;
    }

    /* ------------------- CREATE ------------------- */
    @Transactional
    public CommunicationDto create(CommunicationDto dto) {
        // Validate that both patientId and providerId are provided
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }

        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }

        // Validate that payload is not empty
        if (dto.getPayload() == null || dto.getPayload().trim().isEmpty()) {
            throw new IllegalArgumentException("Message payload cannot be empty");
        }

        String now = LocalDateTime.now().toString();
        
        // Generate externalId if not provided
        String externalId = dto.getExternalId();
        if (externalId == null || externalId.trim().isEmpty()) {
            externalId = UUID.randomUUID().toString();
        }

        // Generate fhirId if not provided
        String fhirId = dto.getFhirId();
        if (fhirId == null || fhirId.trim().isEmpty()) {
            fhirId = UUID.randomUUID().toString();
        }

        Communication entity = Communication.builder()
                .externalId(externalId)
                .fhirId(fhirId)
                .status(dto.getStatus() != null ? dto.getStatus() : CommunicationStatus.SENT)
                .category(dto.getCategory())
                .sentDate(dto.getSentDate() != null ? dto.getSentDate() : now)
                .payload(dto.getPayload())
                .subject(dto.getSubject())
                .inResponseTo(dto.getInResponseTo())
                .patientId(dto.getPatientId())
                .providerId(dto.getProviderId())
                .attachmentIds(dto.getAttachmentIds())
                .messageType(dto.getMessageType())
                .fromType(dto.getFromType())
                .fromId(dto.getFromId())
                .fromName(dto.getFromName())
                .build();

        Communication saved = repo.save(entity);

        log.info("Created communication with id: {} for patient: {} and provider: {}",
                saved.getId(), dto.getPatientId(), dto.getProviderId());
        log.info("Saved entity: id={}, payload={}, patientId={}, providerId={}",
                saved.getId(), saved.getPayload(), saved.getPatientId(), saved.getProviderId());

        CommunicationDto result = toDto(saved);
        log.info("Converted to DTO: {}", result != null ? "success" : "null");

        return result;
    }

    /* ------------------- GET ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getByPatientId(Long patientId) {
        return repo.findAllByPatientId(patientId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ------------------- GET CONVERSATION ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getConversation(Long patientId, Long providerId) {
        return repo.findAllByPatientIdAndProviderId(patientId, providerId)
                .stream()
                .map(this::toDto)
                .sorted((a, b) -> {
                    // Sort by creation date, oldest first for conversation thread
                    if (a.getCreatedDate() != null && b.getCreatedDate() != null) {
                        return a.getCreatedDate().compareTo(b.getCreatedDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommunicationDto getItem(Long patientId, Long id) {
        /*Long orgId = requireOrg("getItem");
        return repo.findAllByPatientIdText(String.valueOf(patientId), String.valueOf(orgId))
                .stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Communication not found: " + id));*/
        return null;
    }

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public CommunicationDto updateItem(Long patientId, Long id, CommunicationDto patch) {
        /*Long orgId = requireOrg("updateItem");
        List<Communication> rows = repo.findAllByPatientIdText(
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
            // Skip external storage updates for communications
            *//*
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.update(toDto(row), row.getExternalId());
            }
            *//*
        }
        return toDto(row);*/
        return null;
    }

    /* ------------------- MARK AS READ ------------------- */
    @Transactional
    public CommunicationDto markAsRead(Long id, String readBy) {
        Communication comm = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        // Only allow marking as read if not already read
        if (comm.getReadAt() == null) {
            String now = LocalDateTime.now().toString();
            comm.setReadAt(now);
            comm.setReadBy(readBy);
            comm.setLastModifiedDate(LocalDateTime.now());
            Communication saved = repo.save(comm);

            // Send read receipt notification to patient if provider read the message
            if ("provider".equals(readBy) && saved.getPatientId() != null) {
                try {
                    sendReadReceiptNotification(saved);
                } catch (Exception e) {
                    log.warn("Failed to send read receipt notification: {}", e.getMessage());
                }
            }

            // Send real-time WebSocket notification for message read
            try {
                if (webSocketController.isPresent()) {
                    CommunicationDto dto = toDto(saved);
                    // webSocketController.get().notifyMessageRead(dto, orgId);
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for message read: {}", e.getMessage());
            }

            return toDto(saved);
        }

        return toDto(comm);
    }

    /**
     * Send read receipt notification to patient when provider reads their message
     */
    private void sendReadReceiptNotification(Communication communication) {
       /* try {
            tenantAwareService.executeInTenantContext(communication.getOrgId(), () -> {
                patientRepo.findById(communication.getPatientId()).ifPresent(patient -> {
                    try {
                        if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isBlank()) {
                            smsService.sendSms(patient.getPhoneNumber(),
                                "Your message has been read by your healthcare provider.");
                        }
                        if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
                            emailService.sendEmail(
                                patient.getEmail(),
                                "Message Read Receipt",
                                "Your message has been read by your healthcare provider."
                            );
                        }
                        log.info("Read receipt notification sent to patient {}", patient.getId());
                    } catch (Exception e) {
                        log.error("Failed to send read receipt notification to patient {}: {}",
                            patient.getId(), e.getMessage());
                    }
                });
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to send read receipt notification: {}", e.getMessage());
        }*/
    }

    /* ------------------- SET STATUS ------------------- */
    @Transactional
    public CommunicationDto setStatus(Long id, CommunicationStatus status) {
        Communication comm = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        comm.setStatus(status);
        Communication saved = repo.save(comm);

        if (saved.getExternalId() != null) {
            // Skip external storage updates for communications
            /*
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.update(toDto(saved), saved.getExternalId());
            }
            */
        }
        return toDto(saved);
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void deleteItem(Long patientId, Long id) {
/*        Long orgId = requireOrg("deleteItem");
        List<Communication> rows = repo.findAllByPatientIdText(
                String.valueOf(patientId), String.valueOf(orgId));
        String externalId = rows.stream().findFirst().map(Communication::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientIdText(
                String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId));
        if (n == 0) throw new RuntimeException("Delete failed: not found");*/

        // if (externalId != null) {
        // Skip external storage deletion for communications
            /*
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                List<Communication> fresh = repo.findAllByPatientIdText(
                        String.valueOf(patientId), String.valueOf(orgId));
                if (fresh.isEmpty()) ext.delete(externalId);
                else ext.update(toDto(fresh.get(0)), externalId);
            }
            */
        //}
    }

    @Transactional
    public void deleteItemById(Long id) {
        Communication row = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Delete failed: not found"));

        String externalId = row.getExternalId();
        repo.delete(row);

        if (externalId != null) {
            // Skip external storage deletion for communications
            /*
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
                ext.delete(externalId);
            }
            */
        }
    }

    /* ------------------- SEARCH ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> searchAll() {
        List<Communication> entities = repo.findAll();
        log.info("Found {} communication entities in database", entities.size());
        List<CommunicationDto> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        log.info("Converted to {} communication DTOs", dtos.size());
        return dtos;
    }    /**
     * Extract user email and org IDs from JWT token
     */
    public Map<String, Object> extractUserInfoFromToken(String token) {
/*        String userEmail = jwtTokenUtil.getEmailFromToken(token);
        List<Long> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", userEmail);
        userInfo.put("orgIds", orgIds);
        return userInfo;*/
        return null;
    }

    /**
     * Get communications for current portal user based on JWT token
     */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getCommunicationsForPortalUser(String token) {
       /* try {
            String userEmail = jwtTokenUtil.getEmailFromToken(token);
            List<Long> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);

            if (orgIds == null || orgIds.isEmpty()) {
                log.error("No orgIds found in token for user {}", userEmail);
                throw new IllegalArgumentException("No organization found in token");
            }

            // Use the first orgId (primary organization)
            Long orgId = ((Number) orgIds.get(0)).longValue();
            log.info("Getting communications for portal user {} in org {}", userEmail, orgId);

            // Get the EHR patient ID for this portal user
            Long patientId = getEhrPatientIdFromPortalUserEmail(userEmail, orgId);
            if (patientId == null) {
                log.warn("No patient ID found for portal user {} in org {}", userEmail, orgId);
                return List.of(); // Return empty list instead of null
            }

            // Get communications using tenant-aware query
            return getCommunicationsByPatient(patientId);

        } catch (Exception e) {
            log.error("Error getting communications for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get communications for user", e);
        }
    }

    // 👩‍⚕️ Portal Method - Map portal user email to EHR patient ID
    @Transactional(readOnly = true)
    public Long getEhrPatientIdFromPortalUserEmail(String email, Long orgId) {
        log.info("Looking up EHR patient ID for portal user {} in org {}", email, orgId);

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return tenantAwareService.executeQueryInMasterContext(em -> {
            try {
                // Query the portal_patients table in public schema to find the EHR patient ID
                Object result = em.createNativeQuery(
                    "SELECT pp.ehr_patient_id FROM public.portal_patients pp " +
                    "JOIN public.portal_users pu ON pp.portal_user_id = pu.id " +
                    "WHERE pu.email = :email LIMIT 1")
                    .setParameter("email", email)
                    .getSingleResult();

                if (result != null) {
                    Long patientId = ((Number) result).longValue();
                    log.info("Found EHR patient ID {} for portal user {} in org {}", patientId, email, orgId);
                    return patientId;
                }
            } catch (Exception e) {
                log.warn("Failed to find EHR patient ID for user {}: {}", email, e.getMessage());
            }

            // Fallback: return patient ID 1 for testing
            log.info("Using fallback patient ID 1 for user {} in org {}", email, orgId);
            return 1L;
        });
    }

    // 🏥 EHR Method - Get all communications for a patient (across all encounters)
    @Transactional(readOnly = true)
    public List<CommunicationDto> getCommunicationsByPatient(Long patientId) {
        log.info("Getting communications for patient {} in org {}", patientId, orgId);

        return tenantAwareService.executeInTenantContext(() -> {
            List<Communication> communications = repo.findAllByPatientIdText(String.valueOf(patientId), String.valueOf(orgId));
            log.info("Found {} communication records for patient {} in org {}", communications.size(), patientId, orgId);
            return communications.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        });*/
        return null;
    }

    /* ------------------- MAPPER ------------------- */
    private CommunicationDto toDto(Communication r) {
        try {
            CommunicationDto dto = new CommunicationDto();
            dto.setId(r.getId());
            dto.setExternalId(r.getExternalId());
            dto.setFhirId(r.getFhirId());
            dto.setStatus(r.getStatus());
            dto.setCategory(r.getCategory());
            dto.setSentDate(r.getSentDate());
            dto.setCreatedDate(r.getCreatedDate() != null ? r.getCreatedDate().toString() : null);
            dto.setLastModifiedDate(r.getLastModifiedDate() != null ? r.getLastModifiedDate().toString() : null);
            dto.setPayload(r.getPayload());
            dto.setSubject(r.getSubject());
            dto.setInResponseTo(r.getInResponseTo()); // Keep as String
            dto.setPatientId(r.getPatientId());
            dto.setProviderId(r.getProviderId());

            // Add read tracking fields
            dto.setReadAt(r.getReadAt());
            dto.setReadBy(r.getReadBy());

            // Determine messageType and fromType from entity or infer from context
            String messageType = r.getMessageType();
            String fromType = r.getFromType();
            
            // If messageType is not set, infer it from the data
            if (messageType == null || messageType.trim().isEmpty()) {
                // Infer from fromId and providerId/patientId
                if (r.getFromId() != null) {
                    if (r.getFromId().equals(r.getProviderId())) {
                        // Provider sent the message
                        messageType = "provider_to_patient";
                        fromType = "provider";
                        log.info("Inferred messageType=provider_to_patient from fromId={} matching providerId={}", 
                                r.getFromId(), r.getProviderId());
                    } else if (r.getFromId().equals(r.getPatientId())) {
                        // Patient sent the message
                        messageType = "patient_to_provider";
                        fromType = "patient";
                        log.info("Inferred messageType=patient_to_provider from fromId={} matching patientId={}", 
                                r.getFromId(), r.getPatientId());
                    }
                }
                
                // Fallback: check sender field format
                if (messageType == null && r.getSender() != null) {
                    if (r.getSender().startsWith("Provider/")) {
                        messageType = "provider_to_patient";
                        fromType = "provider";
                        log.info("Inferred messageType=provider_to_patient from sender={}", r.getSender());
                    } else if (r.getSender().startsWith("Patient/")) {
                        messageType = "patient_to_provider";
                        fromType = "patient";
                        log.info("Inferred messageType=patient_to_provider from sender={}", r.getSender());
                    }
                }
                
                // Last resort: default to patient_to_provider
                if (messageType == null) {
                    messageType = "patient_to_provider";
                    fromType = "patient";
                    log.warn("Could not infer messageType for communication {}, defaulting to patient_to_provider", r.getId());
                }
            }
            
            // Set fromType based on messageType if still not set
            if (fromType == null || fromType.trim().isEmpty()) {
                if ("provider_to_patient".equals(messageType)) {
                    fromType = "provider";
                } else if ("patient_to_provider".equals(messageType)) {
                    fromType = "patient";
                } else {
                    fromType = "patient"; // fallback
                }
            }
            
            dto.setMessageType(messageType);
            dto.setFromType(fromType);
            
            log.info("Communication {}: messageType={}, fromType={}, fromId={}, providerId={}, patientId={}",
                    r.getId(), messageType, fromType, r.getFromId(), r.getProviderId(), r.getPatientId());

            // Set from fields from entity if available
            dto.setFromId(r.getFromId());
            dto.setFromName(r.getFromName());
            
            // Fallback: populate from fields if not set in entity
            if (dto.getFromName() == null || dto.getFromName().trim().isEmpty()) {
                if ("provider".equals(fromType) && r.getProviderId() != null) {
                    try {
                        providerRepo.findById(r.getProviderId()).ifPresent(provider -> {
                            String providerName = provider.getFirstName() + " " + provider.getLastName();
                            dto.setFromId(r.getProviderId());
                            dto.setFromName(providerName);
                        });
                    } catch (Exception e) {
                        log.warn("Could not find provider name for provider ID {}: {}", r.getProviderId(), e.getMessage());
                        dto.setFromName("Unknown Provider");
                    }
                } else if ("patient".equals(fromType) && r.getPatientId() != null) {
                    try {
                        patientRepo.findById(r.getPatientId()).ifPresent(patient -> {
                            String patientName = patient.getFirstName() + " " + patient.getLastName();
                            dto.setFromId(r.getPatientId());
                            dto.setFromName(patientName);
                        });
                    } catch (Exception e) {
                        log.warn("Could not find patient name for patient ID {}: {}", r.getPatientId(), e.getMessage());
                        dto.setFromName("Unknown Patient");
                    }
                }
            }

            // To (patient names)
            List<String> toNames = new ArrayList<>();
            List<Long> toIds = new ArrayList<>();
            if (r.getPatientId() != null) {
                try {
                    patientRepo.findById(r.getPatientId()).ifPresent(patient -> {
                        String patientName = patient.getFirstName() + " " + patient.getLastName();
                        toNames.add(patientName);
                        toIds.add(r.getPatientId());
                    });
                } catch (Exception e) {
                    log.warn("Could not find patient name for patient ID {}: {}", r.getPatientId(), e.getMessage());
                }
            }
            dto.setToNames(toNames);
            dto.setToIds(toIds);

            // Add attachment support
            dto.setAttachmentIds(r.getAttachmentIds());

            // Load message attachments from the dedicated table
            try {
                List<MessageAttachmentDto> messageAttachments = messageAttachmentService.getAllForMessage(r.getId()).getData();
                dto.setAttachments(messageAttachments != null ? messageAttachments : Collections.emptyList());
            } catch (Exception e) {
                log.warn("Failed to load message attachments for communication {}: {}", r.getId(), e.getMessage());
                dto.setAttachments(Collections.emptyList());
            }

            log.info("Successfully converted Communication entity {} to DTO", r.getId());
            return dto;
        } catch (Exception e) {
            log.error("Failed to convert Communication entity {} to DTO: {}", r.getId(), e.getMessage(), e);
            return null;
        }
    }
}
