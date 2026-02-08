package org.ciyex.ehr.service.telehealth;

import org.ciyex.ehr.dto.integration.IntegrationKey;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.dto.integration.TelehealthConfig;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.PracticeContextService;
import org.ciyex.ehr.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@TelehealthVendor("cloudflare")
@Component
@Slf4j
public class CloudflareTelehealthService implements TelehealthService {

    private final OrgIntegrationConfigProvider configProvider;
    private final RestTemplate restTemplate;
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public CloudflareTelehealthService(OrgIntegrationConfigProvider configProvider, 
                                     FhirClientService fhirClientService,
                                     PracticeContextService practiceContextService) {
        this.configProvider = configProvider;
        this.restTemplate = new RestTemplate();
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    public CloudflareMeetingResponse createMeetingWithDetails(CloudflareMeetingRequest request) {
        try {
            // Get Cloudflare config
            TelehealthConfig config = configProvider.get(IntegrationKey.TELEHEALTH);
            log.info("Retrieved telehealth config: {}", config != null ? "present" : "null");
            
            TelehealthConfig.Cloudflare cloudflareConfig = config != null ? config.getCloudflare() : null;
            log.info("Cloudflare config: {}", cloudflareConfig != null ? "present" : "null");
            
            if (cloudflareConfig != null) {
                log.info("AccountId: {}, ApiToken: {}", 
                    cloudflareConfig.getAccountId() != null ? "present" : "null",
                    cloudflareConfig.getApiToken() != null ? "present" : "null");
            }
            
            if (cloudflareConfig == null || isBlank(cloudflareConfig.getAccountId()) || isBlank(cloudflareConfig.getApiToken())) {
                throw new RuntimeException("Cloudflare configuration is missing. Please configure accountId and apiToken in org_integration_config table.");
            }

            // Create real Cloudflare meeting
            CloudflareMeetingApiResponse apiResponse = createCloudflareMeeting(cloudflareConfig, request);
            
            // Store in FHIR
            String fhirId = storeMeetingInFhir(request, apiResponse.roomId(), apiResponse.meetingUrl());
            
            return new CloudflareMeetingResponse(
                apiResponse.roomId(), apiResponse.meetingUrl(), fhirId,
                request.providerId(), request.patientId(),
                request.startTime(), request.endTime(), request.date()
            );
            
        } catch (Exception e) {
            log.error("Failed to create Cloudflare meeting", e);
            throw new RuntimeException("Failed to create meeting: " + e.getMessage(), e);
        }
    }
    
    public CloudflareJoinResponse joinMeetingByRoomId(String roomId) {
        try {
            log.info("Attempting to join meeting with room ID: {}", roomId);
            
            // Get meeting from FHIR
            Appointment meeting = getMeetingFromFhir(roomId);
            if (meeting == null) {
                throw new RuntimeException("Meeting not found for room ID: " + roomId);
            }
            
            // Extract session URL from FHIR
            String sessionUrl = getExtensionValue(meeting, "meetingUrl");
            
            if (isBlank(sessionUrl)) {
                throw new RuntimeException("Session URL not found in FHIR for room: " + roomId);
            }
            
            log.info("Successfully found Cloudflare session for room {}", roomId);
            return new CloudflareJoinResponse(roomId, "participant", sessionUrl);
            
        } catch (Exception e) {
            log.error("Failed to join meeting by room ID: {}", roomId, e);
            throw new RuntimeException("Failed to join meeting: " + e.getMessage(), e);
        }
    }


    private String storeMeetingInFhir(CloudflareMeetingRequest request, String roomId, String meetingUrl) {
        try {
            Appointment appointment = new Appointment();
            appointment.setStatus(Appointment.AppointmentStatus.BOOKED);
            appointment.setDescription("Cloudflare Telehealth Meeting");
            
            // Set participants
            Appointment.AppointmentParticipantComponent provider = new Appointment.AppointmentParticipantComponent();
            provider.setActor(new Reference("Practitioner/" + request.providerId()));
            provider.setStatus(Appointment.ParticipationStatus.ACCEPTED);
            appointment.addParticipant(provider);
            
            Appointment.AppointmentParticipantComponent patient = new Appointment.AppointmentParticipantComponent();
            patient.setActor(new Reference("Patient/" + request.patientId()));
            patient.setStatus(Appointment.ParticipationStatus.ACCEPTED);
            appointment.addParticipant(patient);
            
            // Set time period
            Period period = new Period();
            period.setStart(parseDateTime(request.date(), request.startTime()));
            period.setEnd(parseDateTime(request.date(), request.endTime()));
            appointment.setStart(period.getStart());
            appointment.setEnd(period.getEnd());
            
            // Add custom extensions
            appointment.addExtension("http://ciyex.com/fhir/roomId", new StringType(roomId));
            appointment.addExtension("http://ciyex.com/fhir/meetingUrl", new StringType(meetingUrl));
            appointment.addExtension("http://ciyex.com/fhir/vendor", new StringType("cloudflare"));
            
            var outcome = fhirClientService.create(appointment, practiceContextService.getPracticeId());
            String fhirId = outcome.getId().getIdPart();
            
            log.info("Stored meeting in FHIR with ID: {} for room: {}", fhirId, roomId);
            return fhirId;
            
        } catch (Exception e) {
            log.error("Failed to store meeting in FHIR for room: {}", roomId, e);
            throw new RuntimeException("Failed to store meeting in FHIR: " + e.getMessage(), e);
        }
    }
    
    private Appointment getMeetingFromFhir(String roomId) {
        try {
            Bundle bundle = fhirClientService.search(Appointment.class, practiceContextService.getPracticeId());
            return fhirClientService.extractResources(bundle, Appointment.class).stream()
                .filter(apt -> roomId.equals(getExtensionValue(apt, "roomId")))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get meeting from FHIR for room: {}", roomId, e);
            return null;
        }
    }
    
    private String getExtensionValue(Appointment appointment, String extensionName) {
        Extension ext = appointment.getExtensionByUrl("http://ciyex.com/fhir/" + extensionName);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
    
    private Date parseDateTime(String date, String time) {
        try {
            String dateTimeStr = date + " " + time;
            LocalDateTime ldt = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return java.sql.Timestamp.valueOf(ldt);
        } catch (Exception e) {
            log.warn("Failed to parse date/time: {} {}, using current time", date, time);
            return new Date();
        }
    }
    
    private CloudflareMeetingApiResponse createCloudflareMeeting(TelehealthConfig.Cloudflare config, CloudflareMeetingRequest request) {
        try {
            String accountId = config.getAccountId();
            String apiToken = config.getApiToken();
            String kitId = config.getAppId();
            
            if (isBlank(accountId) || isBlank(apiToken) || isBlank(kitId)) {
                throw new RuntimeException("Cloudflare Account ID, API Token, or Kit ID is missing");
            }
            
            String endpoint = String.format("https://api.cloudflare.com/client/v4/accounts/%s/realtime/kit/%s/meetings", 
                accountId, kitId);
            
            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", "Ciyex Telehealth Session");
            requestBody.put("preferred_region", "ap-south-1");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Call Cloudflare API
            log.info("Creating Cloudflare meeting for provider {} and patient {}", 
                    request.providerId(), request.patientId());
            
            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to create Cloudflare meeting: " + response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
            
            if (result == null) {
                throw new RuntimeException("Invalid response from Cloudflare API: missing result");
            }
            
            String meetingId = (String) result.get("meetingId");
            String meetingUrl = (String) result.get("meetingUrl");
            
            if (isBlank(meetingId)) {
                throw new RuntimeException("Invalid response from Cloudflare API: missing meetingId");
            }
            
            log.info("Successfully created Cloudflare meeting: {} with URL: {}", meetingId, meetingUrl);
            return new CloudflareMeetingApiResponse(meetingId, meetingUrl);
            
        } catch (HttpClientErrorException e) {
            log.error("Cloudflare API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create Cloudflare session: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to create Cloudflare meeting", e);
            throw new RuntimeException("Failed to create Cloudflare meeting: " + e.getMessage(), e);
        }
    }


    // API response record
    private record CloudflareMeetingApiResponse(
        String roomId,      // sessionId from Cloudflare
        String meetingUrl   // cloudflare-realtime://sessionId
    ) {}

    // Records for request/response
    public record CloudflareMeetingRequest(
        Long providerId,
        Long patientId, 
        String startTime,
        String endTime,
        String date
    ) {}
    
    public record CloudflareMeetingResponse(
        String roomId,
        String meetingUrl,
        String fhirId,
        Long providerId,
        Long patientId,
        String startTime,
        String endTime,
        String date
    ) {}
    
    public record CloudflareJoinResponse(
        String roomId,
        String identity, 
        String meetingUrl
    ) {}

    // Legacy methods for backward compatibility
    @Override
    public String startVideoCall(Long providerId, Long patientId, String roomName) {
        String tenantName = getCurrentTenantName();
        String uniqueRoomName = generateUniqueRoomName(roomName, tenantName, providerId, patientId);
        log.info("Started Cloudflare Calls video call for roomName={}, providerId={}, patientId={}",
                uniqueRoomName, providerId, patientId);
        return uniqueRoomName;
    }

    @Override
    public String getCallStatus(String callId) {
        return "active";
    }

    @Override
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        TelehealthConfig config = null;
        try {
            config = configProvider.get(IntegrationKey.TELEHEALTH);
        } catch (Exception e) {
            log.warn("Failed to get telehealth config, using defaults: {}", e.getMessage());
        }
        TelehealthConfig.Cloudflare cloudflareConfig = config != null ? config.getCloudflare() : null;
        if (cloudflareConfig == null || isBlank(cloudflareConfig.getAppId()) || isBlank(cloudflareConfig.getApiToken())) {
            throw new RuntimeException("Cloudflare configuration is missing or incomplete");
        }
        return generateCloudflareToken(cloudflareConfig, roomName, identity, ttlSecs);
    }

    private String generateCloudflareToken(TelehealthConfig.Cloudflare config, String roomName, String identity, Integer ttlSecs) {
        log.info("Using room name as session identifier: {} for identity: {}", roomName, identity);
        return roomName;
    }

    private String generateUniqueRoomName(String baseRoomName, String tenantName, Long providerId, Long patientId) {
        String sanitizedBase = baseRoomName.replaceAll("[^a-zA-Z0-9-]", "-");
        String sanitizedTenant = tenantName != null ? tenantName.replaceAll("[^a-zA-Z0-9-]", "-") : "default";
        return String.format("%s-%s-p%d-pt%d", sanitizedTenant, sanitizedBase, providerId, patientId);
    }

    public CloudflareJoinResponse createJoinTokenWithUrl(String roomName, String identity, Integer ttlSecs) {
        String token = createJoinToken(roomName, identity, ttlSecs);
        return new CloudflareJoinResponse(roomName, identity, token);
    }

    private static boolean isBlank(String s) { 
        return s == null || s.trim().isEmpty(); 
    }

    private String getCurrentTenantName() {
        try {
            RequestContext rc = RequestContext.get();
            return rc != null ? rc.getTenantName() : null;
        } catch (Exception e) {
            log.debug("RequestContext not available, using default tenant for Cloudflare configuration");
            return "practice_1";
        }
    }
}
