package org.ciyex.ehr.service.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.PracticeContextService;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.ciyex.ehr.tabconfig.service.TabFieldConfigService;
import org.hl7.fhir.r4.model.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Portal-scoped wrapper around GenericFhirResourceService.
 *
 * Resolves the current portal user (email from JWT) → FHIR Person → ehr-patient-id extension
 * → linked FHIR Patient ID. Then delegates all CRUD to GenericFhirResourceService using the
 * portal-specific tab_field_config (e.g. "portal-demographics").
 *
 * This ensures:
 *   - Portal and EHR share the SAME FHIR Patient resource (single source of truth)
 *   - Fields are fully configurable via tab_field_config (no hardcoded fields)
 *   - Change detection + notification on updates
 *   - Portal users can only access their own data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalGenericResourceService {

    private final GenericFhirResourceService fhirResourceService;
    private final TabFieldConfigService tabFieldConfigService;
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PortalNotificationService notificationService;
    private final PortalAuthService portalAuthService;
    private final ObjectMapper objectMapper;

    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/portal-status";
    private static final String EXT_EHR_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/ehr-patient-id";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /**
     * Get the field config for a portal tab (returns parsed JSON for frontend rendering).
     */
    public Map<String, Object> getFieldConfig(String tabKey) {
        TabFieldConfig config = tabFieldConfigService.getEffectiveFieldConfig(tabKey, "*", resolveOrgId());
        if (config == null) {
            throw new IllegalArgumentException("No field config found for tab: " + tabKey);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tabKey", config.getTabKey());
        result.put("label", config.getLabel());
        result.put("icon", config.getIcon());
        result.put("fieldConfig", parseJson(config.getFieldConfig()));
        result.put("fhirResources", parseJson(config.getFhirResources()));
        return result;
    }

    /**
     * Get resource data for the current portal user.
     * Resolves email → Person → linked Patient ID → GenericFhirResourceService.
     */
    public Map<String, Object> getData(String tabKey, String email) {
        return getData(tabKey, email, null);
    }

    public Map<String, Object> getData(String tabKey, String email, Jwt jwt) {
        String patientId = jwt != null ? resolvePatientId(email, jwt) : resolvePatientId(email);
        Map<String, Object> data = fhirResourceService.list(tabKey, Long.parseLong(patientId), 0, 1);

        // Mark as single-record for portal (always one Patient per user)
        data.put("singleRecord", true);
        return data;
    }

    /**
     * Update resource data for the current portal user.
     * Detects changes and posts notification.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateData(String tabKey, String email, Map<String, Object> formData) {
        return updateData(tabKey, email, formData, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateData(String tabKey, String email, Map<String, Object> formData, Jwt jwt) {
        String patientId = jwt != null ? resolvePatientId(email, jwt) : resolvePatientId(email);

        // Get old data for change detection
        Map<String, Object> oldData = null;
        try {
            Map<String, Object> existing = fhirResourceService.list(tabKey, Long.parseLong(patientId), 0, 1);
            List<Map<String, Object>> content = (List<Map<String, Object>>) existing.get("content");
            if (content != null && !content.isEmpty()) {
                oldData = content.get(0);
            }
        } catch (Exception e) {
            log.debug("Could not fetch existing data for change detection: {}", e.getMessage());
        }

        // Update via GenericFhirResourceService
        Map<String, Object> result;
        if (oldData != null && oldData.get("id") != null) {
            // Update existing resource
            String resourceId = String.valueOf(oldData.get("id"));
            result = fhirResourceService.update(tabKey, Long.parseLong(patientId), resourceId, formData);
        } else {
            // Create new (shouldn't happen for portal-demographics since Patient exists)
            result = fhirResourceService.create(tabKey, Long.parseLong(patientId), formData);
        }

        // Change detection + notification
        try {
            String patientName = buildPatientName(formData);
            Map<String, String> changes = detectChanges(oldData, formData);
            if (!changes.isEmpty()) {
                notificationService.notifyDemographicsUpdate(patientName, changes);
            }
        } catch (Exception e) {
            log.warn("Failed to post update notification: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Resolve portal user email → linked EHR Patient ID (with JWT for auto-creation).
     * If no FHIR Person exists for this Keycloak user, auto-creates one.
     */
    public String resolvePatientId(String email, Jwt jwt) {
        String practiceId = getPracticeId();
        if (practiceId == null || practiceId.isBlank()) {
            log.warn("No practice/org context available for portal user email={}; " +
                     "ensure X-Org-Alias header or organization JWT claim is present", email);
            throw new IllegalStateException(
                    "Portal account not linked: no organization context. Please contact your provider.");
        }

        Bundle bundle = fhirClientService.search(Person.class, practiceId);
        List<Person> persons = fhirClientService.extractResources(bundle, Person.class);

        Person person = persons.stream()
                .filter(p -> email.equalsIgnoreCase(getEmail(p)))
                .findFirst()
                .orElse(null);

        // Auto-create Person for Keycloak-authenticated patients
        if (person == null && jwt != null) {
            log.info("No portal Person found for Keycloak user {}; auto-creating", email);
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("given_name", jwt.getClaimAsString("given_name") != null ? jwt.getClaimAsString("given_name") : "Unknown");
            userData.put("family_name", jwt.getClaimAsString("family_name") != null ? jwt.getClaimAsString("family_name") : "");
            userData.put("sub", jwt.getSubject());
            person = portalAuthService.ensurePortalUserExistsFromKeycloak(userData, practiceId);
        }

        if (person == null) {
            throw new IllegalArgumentException("Portal user not found for email: " + email);
        }

        return resolvePatientIdFromPerson(person, email, practiceId);
    }

    /**
     * Resolve portal user email → linked EHR Patient ID.
     * Looks up Person by email, checks APPROVED status, extracts ehr-patient-id extension.
     * If no ehr-patient-id is set, attempts auto-link by finding a Patient with matching email.
     */
    public String resolvePatientId(String email) {
        String practiceId = getPracticeId();
        if (practiceId == null || practiceId.isBlank()) {
            log.warn("No practice/org context available for portal user email={}; " +
                     "ensure X-Org-Alias header or organization JWT claim is present", email);
            throw new IllegalStateException(
                    "Portal account not linked: no organization context. Please contact your provider.");
        }
        Bundle bundle = fhirClientService.search(Person.class, practiceId);
        List<Person> persons = fhirClientService.extractResources(bundle, Person.class);

        Person person = persons.stream()
                .filter(p -> email.equalsIgnoreCase(getEmail(p)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Portal user not found for email: " + email));

        return resolvePatientIdFromPerson(person, email, practiceId);
    }

    /**
     * Common logic: given a Person, check APPROVED status, extract/auto-link ehr-patient-id.
     */
    private String resolvePatientIdFromPerson(Person person, String email, String practiceId) {
        // Check approved status
        Extension statusExt = person.getExtensionByUrl(EXT_STATUS);
        if (statusExt == null || !"APPROVED".equals(((StringType) statusExt.getValue()).getValue())) {
            throw new IllegalStateException("Portal user not approved");
        }

        // Get linked EHR Patient ID
        Extension ehrExt = person.getExtensionByUrl(EXT_EHR_PATIENT_ID);
        String ehrPatientId = null;
        if (ehrExt instanceof Extension ext && ext.getValue() instanceof StringType st) {
            ehrPatientId = st.getValue();
        }

        if (ehrPatientId == null || ehrPatientId.isBlank()) {
            // Auto-link: find matching FHIR Patient by email
            ehrPatientId = tryAutoLinkByEmail(email, person, practiceId);
            if (ehrPatientId == null) {
                throw new IllegalStateException("Portal user not linked to EHR patient. Please contact your provider.");
            }
        }

        return ehrPatientId;
    }

    /**
     * Attempt to auto-link a portal Person to an EHR Patient by matching email in telecom.
     * If a match is found, updates the Person with the ehr-patient-id extension and returns the ID.
     */
    private String tryAutoLinkByEmail(String email, Person person, String practiceId) {
        try {
            Bundle patientBundle = fhirClientService.search(Patient.class, practiceId);
            List<Patient> patients = fhirClientService.extractResources(patientBundle, Patient.class);

            Optional<Patient> match = patients.stream()
                    .filter(p -> hasMatchingEmail(p, email))
                    .findFirst();

            if (match.isPresent()) {
                String patientId = match.get().getIdElement().getIdPart();
                log.info("Auto-linking portal user {} to FHIR Patient {}", email, patientId);
                person.getExtension().removeIf(e -> EXT_EHR_PATIENT_ID.equals(e.getUrl()));
                person.addExtension(new Extension(EXT_EHR_PATIENT_ID, new StringType(patientId)));
                fhirClientService.update(person, practiceId);
                return patientId;
            }
            log.warn("No FHIR Patient found with email {} for auto-link", email);
        } catch (Exception e) {
            log.warn("Auto-link attempt failed for email {}: {}", email, e.getMessage());
        }
        return null;
    }

    private boolean hasMatchingEmail(Patient patient, String email) {
        if (!patient.hasTelecom()) return false;
        return patient.getTelecom().stream()
                .anyMatch(t -> ContactPoint.ContactPointSystem.EMAIL.equals(t.getSystem())
                        && email.equalsIgnoreCase(t.getValue()));
    }

    private String getEmail(Person person) {
        if (person.hasTelecom()) {
            return person.getTelecom().stream()
                    .filter(t -> ContactPoint.ContactPointSystem.EMAIL.equals(t.getSystem()))
                    .map(ContactPoint::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String buildPatientName(Map<String, Object> formData) {
        String first = String.valueOf(formData.getOrDefault("firstName", ""));
        String last = String.valueOf(formData.getOrDefault("lastName", ""));
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Patient" : name;
    }

    private Map<String, String> detectChanges(Map<String, Object> oldData, Map<String, Object> newData) {
        Map<String, String> changes = new LinkedHashMap<>();
        if (oldData == null) {
            // New record — report all non-empty fields
            for (Map.Entry<String, Object> entry : newData.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().isBlank()
                        && !entry.getKey().startsWith("_") && !"id".equals(entry.getKey()) && !"fhirId".equals(entry.getKey())) {
                    changes.put(entry.getKey(), entry.getValue().toString());
                }
            }
            return changes;
        }

        // Compare each field
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_") || "id".equals(key) || "fhirId".equals(key)) continue;

            String oldVal = oldData.get(key) != null ? oldData.get(key).toString() : "";
            String newVal = entry.getValue() != null ? entry.getValue().toString() : "";
            if (!oldVal.equals(newVal)) {
                changes.put(key, newVal.isEmpty() ? "(cleared)" : newVal);
            }
        }
        return changes;
    }

    private String resolveOrgId() {
        try {
            return getPracticeId();
        } catch (Exception e) {
            return "*";
        }
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
