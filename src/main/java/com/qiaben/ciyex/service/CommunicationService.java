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

        Communication entity = Communication.builder()
                .status(dto.getStatus() != null ? dto.getStatus() : CommunicationStatus.SENT)
                .category(dto.getCategory())
                .sentDate(dto.getSentDate() != null ? dto.getSentDate() : now)
                .payload(dto.getPayload())
                .subject(dto.getSubject())
                .inResponseTo(dto.getInResponseTo())
                .patientId(dto.getPatientId())
                .providerId(dto.getProviderId())
                .attachmentIds(dto.getAttachmentIds())
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

    @Transactional(readOnly = true)
    public CommunicationDto getItem(Long patientId, Long id) {
        return repo.findById(id)
                .filter(c -> Objects.equals(c.getPatientId(), patientId))
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Communication not found: " + id));
    }

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public CommunicationDto updateItem(Long patientId, Long id, CommunicationDto patch) {
        Communication row = repo.findById(id)
                .filter(c -> Objects.equals(c.getPatientId(), patientId))
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        if (patch.getPayload() != null) row.setPayload(patch.getPayload());
        if (patch.getStatus() != null) row.setStatus(patch.getStatus());
        row.setLastModifiedDate(LocalDateTime.now());
        repo.save(row);

        return toDto(row);
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
                sendReadReceiptNotification(saved);
            }

            return toDto(saved);
        }

        return toDto(comm);
    }

    /**
     * Send read receipt notification to patient when provider reads their message
     */
    private void sendReadReceiptNotification(Communication communication) {
        try {
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
        } catch (Exception e) {
            log.error("Failed to send read receipt notification: {}", e.getMessage());
        }
    }

    /* ------------------- SET STATUS ------------------- */
    @Transactional
    public CommunicationDto setStatus(Long id, CommunicationStatus status) {
        Communication comm = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Communication not found id=" + id));

        comm.setStatus(status);
        Communication saved = repo.save(comm);

        return toDto(saved);
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void deleteItem(Long patientId, Long id) {
        Communication comm = repo.findById(id)
                .filter(c -> Objects.equals(c.getPatientId(), patientId))
                .orElseThrow(() -> new RuntimeException("Communication not found or access denied"));
        repo.delete(comm);
    }

    @Transactional
    public void deleteItemById(Long id) {
        Communication row = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Delete failed: not found"));

        repo.delete(row);
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
    }

    /**
     * Get communications for current portal user based on email
     */
    @Transactional(readOnly = true)
    public List<CommunicationDto> getCommunicationsForPortalUser(String email) {
        try {
            log.info("Getting communications for portal user {}", email);

            // Get the EHR patient ID for this portal user
            Long patientId = getEhrPatientIdFromPortalUserEmail(email);
            if (patientId == null) {
                log.warn("No patient ID found for portal user {}", email);
                return List.of(); // Return empty list instead of null
            }

            // Get communications for the patient
            return getCommunicationsByPatient(patientId);

        } catch (Exception e) {
            log.error("Error getting communications for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get communications for user", e);
        }
    }

    // 👩‍⚕️ Portal Method - Map portal user email to EHR patient ID
    @Transactional(readOnly = true)
    public Long getEhrPatientIdFromPortalUserEmail(String email) {
        log.info("Looking up EHR patient ID for portal user {}", email);

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            // Query the portal_patients table to find the EHR patient ID
            Object result = entityManager.createNativeQuery(
                "SELECT pp.ehr_patient_id FROM portal_patients pp " +
                "JOIN portal_users pu ON pp.portal_user_id = pu.id " +
                "WHERE pu.email = :email LIMIT 1")
                .setParameter("email", email)
                .getSingleResult();

            if (result != null) {
                Long patientId = ((Number) result).longValue();
                log.info("Found EHR patient ID {} for portal user {}", patientId, email);
                return patientId;
            }
        } catch (Exception e) {
            log.warn("Failed to find EHR patient ID for user {}: {}", email, e.getMessage());
        }

        // Fallback: return patient ID 1 for testing
        log.info("Using fallback patient ID 1 for user {}", email);
        return 1L;
    }

    // 🏥 EHR Method - Get all communications for a patient (across all encounters)
    @Transactional(readOnly = true)
    public List<CommunicationDto> getCommunicationsByPatient(Long patientId) {
        log.info("Getting communications for patient {}", patientId);

        List<Communication> communications = repo.findAllByPatientId(patientId);
        log.info("Found {} communication records for patient {}", communications.size(), patientId);
        return communications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ------------------- MAPPER ------------------- */
    private CommunicationDto toDto(Communication r) {
        try {
            CommunicationDto dto = new CommunicationDto();
            dto.setId(r.getId());
            dto.setExternalId(r.getExternalId());
            dto.setStatus(r.getStatus());
            dto.setCategory(r.getCategory());
            dto.setSentDate(r.getSentDate());
            dto.setCreatedDate(r.getCreatedDate() != null ? r.getCreatedDate().toString() : null);
            dto.setLastModifiedDate(r.getLastModifiedDate() != null ? r.getLastModifiedDate().toString() : null);
            dto.setPayload(r.getPayload());
            dto.setSubject(r.getSubject());
            dto.setInResponseTo(r.getInResponseTo());
            dto.setPatientId(r.getPatientId());
            dto.setProviderId(r.getProviderId());

            // Add read tracking fields
            dto.setReadAt(r.getReadAt());
            dto.setReadBy(r.getReadBy());

            // Determine message type and sender based on the 'sender' field
            String messageType = "unknown";
            Long fromId = null;
            final String[] fromName = {null};

            if (r.getSender() != null) {
                if (r.getSender().startsWith("Patient/")) {
                    messageType = "patient_to_provider";
                    fromId = r.getPatientId();
                    // Get patient name
                    if (r.getPatientId() != null) {
                        try {
                            patientRepo.findById(r.getPatientId()).ifPresent(patient -> {
                                fromName[0] = patient.getFirstName() + " " + patient.getLastName();
                            });
                        } catch (Exception e) {
                            log.warn("Could not find patient name for patient ID {}: {}", r.getPatientId(), e.getMessage());
                            fromName[0] = "Unknown Patient";
                        }
                    }
                } else if (r.getSender().startsWith("Provider/")) {
                    messageType = "provider_to_patient";
                    fromId = r.getProviderId();
                    // Get provider name
                    if (r.getProviderId() != null) {
                        try {
                            providerRepo.findById(r.getProviderId()).ifPresent(provider -> {
                                fromName[0] = provider.getFirstName() + " " + provider.getLastName();
                            });
                        } catch (Exception e) {
                            log.warn("Could not find provider name for provider ID {}: {}", r.getProviderId(), e.getMessage());
                            fromName[0] = "Unknown Provider";
                        }
                    }
                }
            }

            dto.setMessageType(messageType);
            dto.setFromId(fromId);
            dto.setFromName(fromName[0]);

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
