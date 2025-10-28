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
        /*String now = LocalDateTime.now().toString();

        Long providerId = communicationDto.getProviderId();
        if (providerId == null && communicationDto.getSender() != null && communicationDto.getSender().startsWith("Provider/")) {
            try {
                providerId = Long.valueOf(communicationDto.getSender().split("/")[1]);
            } catch (Exception e) {
                log.warn("Invalid provider sender: {}", communicationDto.getSender());
            }
        }

        Long patientId = communicationDto.getPatientId();
        if (patientId == null && communicationDto.getRecipients() != null) {
            for (String rec : communicationDto.getRecipients()) {
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
                .orgId(orgId)
                .status(communicationDto.getStatus() != null ? communicationDto.getStatus() : CommunicationStatus.SENT)
                .category(communicationDto.getCategory())
                .sentDate(communicationDto.getSentDate() != null ? communicationDto.getSentDate() : now)
                .createdDate(now)
                .lastModifiedDate(now)
                .payload(communicationDto.getPayload())
                .sender(communicationDto.getSender())
                .recipients(communicationDto.getRecipients() != null ? String.join(",", communicationDto.getRecipients()) : null)
                .subject(communicationDto.getSubject())
                .inResponseTo(communicationDto.getInResponseTo())
                .patientId(patientId)
                .providerId(providerId)
                .attachmentIds(communicationDto.getAttachmentIds())
                .build();

        // Use tenant-aware context for saving
        Communication saved = tenantAwareService.executeInTenantContext(() -> repo.save(entity));

        // Skip external storage for communications in development/local environment
        // Communications are stored locally in the database and don't need FHIR external storage
        *//*
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<CommunicationDto> ext = storageResolver.resolve(CommunicationDto.class);
            CommunicationDto snap = toDto(saved);
            String externalId = ext.create(snap);
            saved.setExternalId(externalId);
            // Update external ID in tenant context
            tenantAwareService.executeInTenantContext(() -> repo.save(saved));
        }
        *//*

        // send SMS & Email notifications - only for provider-to-patient messages
        if (saved.getPatientId() != null && saved.getProviderId() != null &&
            !(saved.getSender() != null && saved.getSender().startsWith("Patient/"))) {
            // Get patient in tenant context
            tenantAwareService.executeInTenantContext(() -> {
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
                return null;
            });
        }

        // Send real-time WebSocket notification for new message
        try {
            if (webSocketController.isPresent()) {
                CommunicationDto dto = toDto(saved);
                webSocketController.get().notifyNewMessage(dto, orgId);
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for new message: {}", e.getMessage());
        }

        return toDto(saved);*/
        return null;
    }

    /* ------------------- GET ------------------- */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getByPatientId(Long patientId) {
        /*return repo.findAllByPatientIdText(String.valueOf(patientId), String.valueOf(orgId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());*/
        return null;
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
       /* Long orgId = requireOrg("markAsRead");
        Communication comm = repo.findById(id)
                .filter(c -> Objects.equals(c.getOrgId(), orgId))
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        // Only allow marking as read if not already read
        if (comm.getReadAt() == null) {
            String now = LocalDateTime.now().toString();
            comm.setReadAt(now);
            comm.setReadBy(readBy);
            comm.setLastModifiedDate(now);
            Communication saved = repo.save(comm);

            // Send read receipt notification to patient if provider read the message
            if ("provider".equals(readBy) && saved.getPatientId() != null) {
                sendReadReceiptNotification(saved);
            }

            // Send real-time WebSocket notification for message read
            try {
                if (webSocketController.isPresent()) {
                    CommunicationDto dto = toDto(saved);
                    webSocketController.get().notifyMessageRead(dto, orgId);
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for message read: {}", e.getMessage());
            }

            return toDto(saved);
        }

        return toDto(comm);*/
        return null;
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
/*        Long orgId = requireOrg("searchAll");
        return repo.findText(String.valueOf(orgId))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());*/
        return null;
    }

    /**
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
        /*CommunicationDto dto = new CommunicationDto();
        dto.setId(r.getId());
        dto.setExternalId(r.getExternalId());
        dto.setStatus(r.getStatus());
        dto.setCategory(r.getCategory());
        dto.setSentDate(r.getSentDate());
        dto.setPayload(r.getPayload());
        dto.setSender(r.getSender());
        dto.setRecipients(r.getRecipients() != null ? Arrays.asList(r.getRecipients().split(",")) : Collections.emptyList());
        dto.setSubject(r.getSubject());
        dto.setInResponseTo(r.getInResponseTo());
        dto.setPatientId(r.getPatientId());
        dto.setProviderId(r.getProviderId());

        // Add read tracking fields
        dto.setReadAt(r.getReadAt());
        dto.setReadBy(r.getReadBy());

        // Determine message type based on sender and provider_id
        String messageType = "unknown";
        if (r.getSender() != null && r.getSender().startsWith("Patient/")) {
            // If sender starts with "Patient/", it's a patient-to-provider message
            messageType = "patient_to_provider";
        } else if (r.getSender() != null && r.getSender().startsWith("Provider/")) {
            // If sender starts with "Provider/", it's a provider-to-patient message
            messageType = "provider_to_patient";
        } else if (r.getProviderId() != null) {
            // If provider_id is set and sender is not specified as Patient/, assume provider-to-patient
            messageType = "provider_to_patient";
        }
        dto.setMessageType(messageType);

        // From (provider name)
        if (r.getProviderId() != null) {
            // Query provider in current tenant schema

                String providerName;
                    try {
                        Object[] result = (Object[]) entityManager.createNativeQuery(
                            "SELECT first_name, last_name FROM .provider WHERE id = :providerId")
                            .setParameter("providerId", r.getProviderId())
                            .getSingleResult();
                        if (result != null && result.length >= 2) {
                            providerName = result[0] + " " + result[1];
                        }
                    } catch (Exception e) {
                        log.warn("Could not find provider name for provider ID {} in schema {}: {}", r.getProviderId(), e.getMessage());
                    }
                if (providerName != null) {
                    dto.setFromName(providerName);
                }

        } else if (r.getSender() != null && r.getSender().startsWith("Provider/")) {
            try {
                Long pid = Long.valueOf(r.getSender().split("/")[1]);
                // Query provider in current tenant schema
                Long orgId = r.getOrgId();
                if (orgId != null) {
                    String schemaName = "practice_" + orgId;
                    String providerName =
                        try {
                            Object[] result = (Object[]) entityManager.createNativeQuery(
                                "SELECT first_name, last_name FROM " + schemaName + ".provider WHERE id = :providerId")
                                .setParameter("providerId", pid)
                                .getSingleResult();
                            if (result != null && result.length >= 2) {
                                return result[0] + " " + result[1];
                            }
                        } catch (Exception e) {
                            log.warn("Could not find provider name for provider ID {} in schema {}: {}", pid, schemaName, e.getMessage());
                        }
                        return null;

                    if (providerName != null) {
                        dto.setFromName(providerName);
                    }
                }
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

        // Add attachment support
        dto.setAttachmentIds(r.getAttachmentIds());

        // Load message attachments from the dedicated table
        try {
            List<MessageAttachmentDto> messageAttachments = tenantAwareService.executeInTenantContext(r.getOrgId(), () -> {
                ApiResponse<List<MessageAttachmentDto>> response = messageAttachmentService.getAllForMessage(r.getOrgId(), r.getId());
                return response.getData() != null ? response.getData() : Collections.emptyList();
            });
            dto.setAttachments(messageAttachments);
        } catch (Exception e) {
            log.warn("Failed to load message attachments for communication {}: {}", r.getId(), e.getMessage());
            dto.setAttachments(Collections.emptyList());
        }

        return dto;*/
        return null;
    }
}
