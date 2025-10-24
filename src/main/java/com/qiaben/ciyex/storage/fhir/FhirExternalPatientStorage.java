package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalPatientStorage")
@Slf4j
public class FhirExternalPatientStorage implements ExternalStorage<PatientDto> {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public FhirExternalPatientStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalPatientStorage with FhirClientProvider");
    }

    @Override
    public String create(PatientDto entityDto) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering create for tenantName: {}, patientName: {}", tenantName, getPatientName(entityDto));
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Patient fhirPatient = mapToFhirPatient(entityDto);
            log.debug("Mapped PatientDto to FHIR Patient: name={}, mrn={}", fhirPatient.getNameFirstRep(), entityDto.getMedicalRecordNumber());
            String externalId = client.create().resource(fhirPatient).execute().getId().getIdPart();
            log.info("Created Patient with externalId: {} for tenantName: {}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(PatientDto entityDto, String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering update for tenantName: {}, externalId: {}, patientName: {}", tenantName, externalId, getPatientName(entityDto));
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Patient fhirPatient = mapToFhirPatient(entityDto);
            fhirPatient.setId(externalId);
            log.debug("Updating FHIR Patient with id: {}, name={}, mrn={}", externalId, fhirPatient.getNameFirstRep(), entityDto.getMedicalRecordNumber());
            client.update().resource(fhirPatient).execute();
            log.info("Updated Patient with externalId: {} for tenantName: {}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public PatientDto get(String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering get for tenantName: {}, externalId: {}", tenantName, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Patient fhirPatient = client.read().resource(Patient.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Patient with id: {}, name={}", externalId, fhirPatient.getNameFirstRep());
            PatientDto patientDto = mapFromFhirPatient(fhirPatient);
            log.info("Retrieved PatientDto with externalId: {} for tenantName: {}", externalId, tenantName);
            log.debug("Mapped PatientDto: externalId={}, name={}, mrn={}", patientDto.getExternalId(), getPatientName(patientDto), patientDto.getMedicalRecordNumber());
            return patientDto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering delete for tenantName: {}, externalId: {}", tenantName, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            log.info("Deleting Patient with externalId: {} for tenantName: {}", externalId, tenantName);
            client.delete().resourceById("Patient", externalId).execute();
            log.info("Deleted Patient with externalId: {} for tenantName: {}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<PatientDto> searchAll() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering searchAll for tenantName: {}", tenantName);
        if (tenantName == null) {
            log.warn("tenantName is null in RequestContext, defaulting to no filtering");
        }

        Bundle bundle = fhirClientProvider.getForCurrentTenant().search()
                .forResource(org.hl7.fhir.r4.model.Patient.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", tenantName != null ? tenantName : ""))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for tenantName: {}", bundle.getEntry().size(), tenantName);
        List<PatientDto> patientDtos = bundle.getEntry().stream()
                .map(entry -> {
                    Patient patient = (Patient) entry.getResource();
                    log.debug("Processing Patient entry: id={}, name={}", patient.getIdElement().getIdPart(), patient.getNameFirstRep());
                    PatientDto dto = new PatientDto();
                    dto.setExternalId(patient.getIdElement().getIdPart());
                    if (!patient.getName().isEmpty()) {
                        HumanName name = patient.getNameFirstRep();
                        dto.setFirstName(name.getGivenAsSingleString());
                        dto.setLastName(name.getFamily());
                        dto.setMiddleName(name.getGiven().size() > 1 ? name.getGiven().get(1).getValue() : null);
                    }
                    dto.setGender(patient.getGender() != null ? patient.getGender().getDisplay() : null);
                    dto.setDateOfBirth(patient.getBirthDate() != null ? DATE_FORMAT.format(patient.getBirthDate()) : null);
                    dto.setPhoneNumber(patient.getTelecom().stream()
                            .filter(t -> t.getSystem() == org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
                            .findFirst().map(org.hl7.fhir.r4.model.ContactPoint::getValue).orElse(null));
                    dto.setEmail(patient.getTelecom().stream()
                            .filter(t -> t.getSystem() == org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.EMAIL)
                            .findFirst().map(org.hl7.fhir.r4.model.ContactPoint::getValue).orElse(null));
                    dto.setAddress(patient.getAddressFirstRep().getLine().stream().findFirst().map(StringType::getValue).orElse(null));
                    dto.setMedicalRecordNumber(patient.getIdentifierFirstRep().getValue());
                    dto.setTenantName(tenantName); // Set tenantName from RequestContext

                    // Mapping demographics (extensions)
                    patient.getExtension().forEach(extension -> {
                        switch (extension.getUrl()) {
                            case "http://example.org/fhir/preferredName":
                                dto.setPreferredName(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/licenseId":
                                dto.setLicenseId(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/sexualOrientation":
                                dto.setSexualOrientation(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/emergencyContact":
                                dto.setEmergencyContact(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/race":
                                dto.setRace(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/ethnicity":
                                dto.setEthnicity(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/guardianName":
                                dto.setGuardianName(extension.getValue().toString());
                                break;
                            case "http://example.org/fhir/guardianRelationship":
                                dto.setGuardianRelationship(extension.getValue().toString());
                                break;
                        }
                    });

                    log.debug("Mapped PatientDto: externalId={}, name={}, mrn={}", dto.getExternalId(), getPatientName(dto), dto.getMedicalRecordNumber());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Retrieved {} patients for tenantName: {} after mapping", patientDtos.size(), tenantName);
        return patientDtos;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return PatientDto.class.isAssignableFrom(entityType);
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Entering executeWithRetry for tenantName: {}", tenantName);
        try {
            T result = operation.execute();
            log.debug("executeWithRetry succeeded for tenantName: {}", tenantName);
            return result;
        } catch (FhirClientConnectionException e) {
            log.error("FhirClientConnectionException for tenantName: {} with status: {}, message: {}", tenantName, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("Received 401, retrying with fresh FHIR client for tenantName: {}", tenantName);
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in executeWithRetry for tenantName: {}, message: {}, stacktrace: {}", tenantName, e.getMessage(), e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Patient mapToFhirPatient(PatientDto patientDto) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Mapping PatientDto to FHIR Patient for tenantName: {}, name: {}, mrn: {}", tenantName, getPatientName(patientDto), patientDto.getMedicalRecordNumber());
        Patient fhirPatient = new Patient();
        if (patientDto.getFirstName() != null || patientDto.getLastName() != null) {
            HumanName name = new HumanName();
            name.addGiven(patientDto.getFirstName());
            if (patientDto.getMiddleName() != null) name.addGiven(patientDto.getMiddleName());
            if (patientDto.getLastName() != null) name.setFamily(patientDto.getLastName());
            fhirPatient.setName(List.of(name));
        }
        if (patientDto.getGender() != null) {
            String normalized = normalizeGenderCode(patientDto.getGender());
            try {
                fhirPatient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.fromCode(normalized));
            } catch (Exception e) {
                log.warn("Unknown AdministrativeGender code '{}' after normalization, leaving null", patientDto.getGender());
            }
        }
        if (patientDto.getDateOfBirth() != null) {
            try {
                fhirPatient.setBirthDate(DATE_FORMAT.parse(patientDto.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Failed to parse dateOfBirth: {}, using null", patientDto.getDateOfBirth(), e);
            }
        }
        if (patientDto.getPhoneNumber() != null) fhirPatient.addTelecom(new org.hl7.fhir.r4.model.ContactPoint().setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE).setValue(patientDto.getPhoneNumber()));
        if (patientDto.getEmail() != null) fhirPatient.addTelecom(new org.hl7.fhir.r4.model.ContactPoint().setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.EMAIL).setValue(patientDto.getEmail()));
        if (patientDto.getAddress() != null) {
            org.hl7.fhir.r4.model.Address address = new org.hl7.fhir.r4.model.Address();
            address.addLine(patientDto.getAddress());
            fhirPatient.setAddress(List.of(address));
        }
        if (patientDto.getMedicalRecordNumber() != null) fhirPatient.addIdentifier().setValue(patientDto.getMedicalRecordNumber());

        // Mapping demographics fields (extensions)
        if (patientDto.getPreferredName() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/preferredName", new StringType(patientDto.getPreferredName())));
        }
        if (patientDto.getLicenseId() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/licenseId", new StringType(patientDto.getLicenseId())));
        }
        if (patientDto.getSexualOrientation() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/sexualOrientation", new StringType(patientDto.getSexualOrientation())));
        }
        if (patientDto.getEmergencyContact() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/emergencyContact", new StringType(patientDto.getEmergencyContact())));
        }
        if (patientDto.getRace() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/race", new StringType(patientDto.getRace())));
        }
        if (patientDto.getEthnicity() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/ethnicity", new StringType(patientDto.getEthnicity())));
        }
        if (patientDto.getGuardianName() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/guardianName", new StringType(patientDto.getGuardianName())));
        }
        if (patientDto.getGuardianRelationship() != null) {
            fhirPatient.addExtension(new Extension("http://example.org/fhir/guardianRelationship", new StringType(patientDto.getGuardianRelationship())));
        }

        log.debug("Mapped FHIR Patient for tenantName: {}, name: {}, mrn: {}", tenantName, getPatientName(patientDto), patientDto.getMedicalRecordNumber());
        return fhirPatient;
    }

    /**
     * Accept common short codes and variants and convert them to FHIR administrative gender codes.
     * Examples: "M" -> "male", "F" -> "female", "O" -> "other", "U" -> "unknown".
     */
    private String normalizeGenderCode(String gender) {
        if (gender == null) return null;
        String g = gender.trim().toLowerCase();
        if (g.isEmpty()) return g;

        switch (g) {
            case "m":
            case "male":
                return "male";
            case "f":
            case "female":
                return "female";
            case "o":
            case "other":
                return "other";
            case "u":
            case "unknown":
                return "unknown";
            default:
                // Return as-is (lowercased) to allow full codes like "unknown" or custom codes to pass through
                return g;
        }
    }

    private PatientDto mapFromFhirPatient(Patient fhirPatient) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Mapping FHIR Patient to PatientDto for tenantName: {}, id: {}, name: {}", tenantName, fhirPatient.getIdElement().getIdPart(), fhirPatient.getNameFirstRep());
        PatientDto dto = new PatientDto();
        dto.setExternalId(fhirPatient.getIdElement().getIdPart());
        if (!fhirPatient.getName().isEmpty()) {
            HumanName name = fhirPatient.getNameFirstRep();
            dto.setFirstName(name.getGivenAsSingleString());
            dto.setLastName(name.getFamily());
            dto.setMiddleName(name.getGiven().size() > 1 ? name.getGiven().get(1).getValue() : null);
        }
        dto.setGender(fhirPatient.getGender() != null ? fhirPatient.getGender().getDisplay() : null);
        dto.setDateOfBirth(fhirPatient.getBirthDate() != null ? DATE_FORMAT.format(fhirPatient.getBirthDate()) : null);
        dto.setPhoneNumber(fhirPatient.getTelecom().stream()
                .filter(t -> t.getSystem() == org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
                .findFirst().map(org.hl7.fhir.r4.model.ContactPoint::getValue).orElse(null));
        dto.setEmail(fhirPatient.getTelecom().stream()
                .filter(t -> t.getSystem() == org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.EMAIL)
                .findFirst().map(org.hl7.fhir.r4.model.ContactPoint::getValue).orElse(null));
        dto.setAddress(fhirPatient.getAddressFirstRep().getLine().stream().findFirst().map(StringType::getValue).orElse(null));
        dto.setMedicalRecordNumber(fhirPatient.getIdentifierFirstRep().getValue());
        dto.setTenantName(tenantName); // Set tenantName from RequestContext

        // Map demographics fields (extensions)
        fhirPatient.getExtension().forEach(extension -> {
            switch (extension.getUrl()) {
                case "http://example.org/fhir/preferredName":
                    dto.setPreferredName(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/licenseId":
                    dto.setLicenseId(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/sexualOrientation":
                    dto.setSexualOrientation(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/emergencyContact":
                    dto.setEmergencyContact(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/race":
                    dto.setRace(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/ethnicity":
                    dto.setEthnicity(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/guardianName":
                    dto.setGuardianName(extension.getValue().toString());
                    break;
                case "http://example.org/fhir/guardianRelationship":
                    dto.setGuardianRelationship(extension.getValue().toString());
                    break;
            }
        });

        log.debug("Mapped PatientDto for tenantName: {}, externalId: {}, name: {}, mrn: {}", tenantName, dto.getExternalId(), getPatientName(dto), dto.getMedicalRecordNumber());
        return dto;
    }

    private String getPatientName(PatientDto patientDto) {
        return (patientDto.getFirstName() != null ? patientDto.getFirstName() : "") + " " +
                (patientDto.getLastName() != null ? patientDto.getLastName() : "");
    }
}
