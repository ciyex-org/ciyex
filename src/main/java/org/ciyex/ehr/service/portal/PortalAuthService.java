package org.ciyex.ehr.service.portal;

import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.dto.portal.AuditDto;
import org.ciyex.ehr.dto.portal.PortalLoginRequest;
import org.ciyex.ehr.dto.portal.PortalLoginResponse;
import org.ciyex.ehr.dto.portal.PortalRegisterRequest;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.PracticeContextService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * FHIR-only Portal Auth Service.
 * Uses FHIR Person resource for portal user authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalAuthService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final long JWT_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours

    private static final String EXT_PASSWORD = "http://ciyex.com/fhir/StructureDefinition/password-hash";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/portal-status";
    private static final String EXT_APPROVED_DATE = "http://ciyex.com/fhir/StructureDefinition/approved-date";
    private static final String EXT_KEYCLOAK_ID = "http://ciyex.com/fhir/StructureDefinition/keycloak-user-id";
    private static final String EXT_DOB = "http://ciyex.com/fhir/StructureDefinition/date-of-birth";
    private static final String EXT_STREET = "http://ciyex.com/fhir/StructureDefinition/street";
    private static final String EXT_STREET2 = "http://ciyex.com/fhir/StructureDefinition/street2";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_POSTAL_CODE = "http://ciyex.com/fhir/StructureDefinition/postal-code";
    private static final String EXT_COUNTRY = "http://ciyex.com/fhir/StructureDefinition/country";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    private String generateJwtToken(Person person) {
        String fhirId = person.getIdElement().getIdPart();
        String email = getEmail(person);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", fhirId);
        claims.put("email", email);
        claims.put("role", "PATIENT");
        claims.put("status", getStringExt(person, EXT_STATUS));
        claims.put("preferred_username", email);

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Collections.singletonList("PATIENT"));
        claims.put("realm_access", realmAccess);

        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();
    }

    public ApiResponse<PortalLoginResponse> register(PortalRegisterRequest request) {
        try {
            // Check if email already exists
            Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
            boolean emailExists = fhirClientService.extractResources(bundle, Person.class).stream()
                    .anyMatch(p -> request.getEmail().equalsIgnoreCase(getEmail(p)));

            if (emailExists) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Email already registered")
                        .build();
            }

            // Create Person resource
            Person person = new Person();
            person.addName()
                    .setFamily(request.getLastName())
                    .addGiven(request.getFirstName());
            person.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(request.getEmail());
            if (request.getPhoneNumber() != null) {
                person.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(request.getPhoneNumber());
            }

            // Password hash
            person.addExtension(new Extension(EXT_PASSWORD, new StringType(passwordEncoder.encode(request.getPassword()))));
            person.addExtension(new Extension(EXT_STATUS, new StringType("APPROVED")));
            person.addExtension(new Extension(EXT_APPROVED_DATE, new DateTimeType(new Date())));

            // Address
            if (request.getStreet() != null) {
                person.addExtension(new Extension(EXT_STREET, new StringType(request.getStreet())));
            }
            if (request.getStreet2() != null) {
                person.addExtension(new Extension(EXT_STREET2, new StringType(request.getStreet2())));
            }
            if (request.getCity() != null) {
                person.addExtension(new Extension(EXT_CITY, new StringType(request.getCity())));
            }
            if (request.getState() != null) {
                person.addExtension(new Extension(EXT_STATE, new StringType(request.getState())));
            }
            if (request.getPostalCode() != null) {
                person.addExtension(new Extension(EXT_POSTAL_CODE, new StringType(request.getPostalCode())));
            }
            person.addExtension(new Extension(EXT_COUNTRY, new StringType(request.getCountry() != null ? request.getCountry() : "USA")));

            // DOB
            if (request.getDateOfBirth() != null) {
                person.addExtension(new Extension(EXT_DOB, new DateType(Date.from(request.getDateOfBirth().atStartOfDay(ZoneId.systemDefault()).toInstant()))));
            }

            var outcome = fhirClientService.create(person, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            PortalLoginResponse response = buildLoginResponse(person, fhirId);

            return ApiResponse.<PortalLoginResponse>builder()
                    .success(true)
                    .message("Registration successful and approved!")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Registration failed", e);
            return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    public ApiResponse<PortalLoginResponse> login(PortalLoginRequest request) {
        try {
            Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
            Optional<Person> userOpt = fhirClientService.extractResources(bundle, Person.class).stream()
                    .filter(p -> request.getEmail().equalsIgnoreCase(getEmail(p)))
                    .findFirst();

            if (userOpt.isEmpty()) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            Person person = userOpt.get();
            String passwordHash = getStringExt(person, EXT_PASSWORD);

            if (passwordHash == null || !passwordEncoder.matches(request.getPassword(), passwordHash)) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            String status = getStringExt(person, EXT_STATUS);
            if (!"APPROVED".equals(status)) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("Your account is not approved yet")
                        .build();
            }

            String fhirId = person.getIdElement().getIdPart();
            PortalLoginResponse response = buildLoginResponse(person, fhirId);
            response.setToken(generateJwtToken(person));

            return ApiResponse.<PortalLoginResponse>builder()
                    .success(true)
                    .message("Login successful")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Login failed", e);
            return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("Login failed: " + e.getMessage())
                    .build();
        }
    }

    public Person ensurePortalUserExistsFromKeycloak(Map<String, Object> userData) {
        String email = (String) userData.getOrDefault("email", "");
        String firstName = (String) userData.getOrDefault("given_name", "Unknown");
        String lastName = (String) userData.getOrDefault("family_name", "");
        String keycloakId = (String) userData.getOrDefault("sub", null);

        if (email.isEmpty()) {
            throw new RuntimeException("Email missing in Keycloak token");
        }

        Bundle bundle = fhirClientService.search(Person.class, getPracticeId());
        Optional<Person> existingOpt = fhirClientService.extractResources(bundle, Person.class).stream()
                .filter(p -> email.equalsIgnoreCase(getEmail(p)))
                .findFirst();

        if (existingOpt.isPresent()) {
            Person existing = existingOpt.get();
            String currentKeycloakId = getStringExt(existing, EXT_KEYCLOAK_ID);

            if (currentKeycloakId == null || !currentKeycloakId.equals(keycloakId)) {
                log.info("Updating existing user {} with Keycloak UUID {}", email, keycloakId);
                existing.getExtension().removeIf(e -> EXT_KEYCLOAK_ID.equals(e.getUrl()));
                existing.addExtension(new Extension(EXT_KEYCLOAK_ID, new StringType(keycloakId)));
                fhirClientService.update(existing, getPracticeId());
            }

            return existing;
        }

        // Create new user
        log.info("Creating portal user for new Keycloak user {}", email);

        Person newPerson = new Person();
        newPerson.addName().setFamily(lastName).addGiven(firstName);
        newPerson.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(email);
        newPerson.addExtension(new Extension(EXT_PASSWORD, new StringType(passwordEncoder.encode(UUID.randomUUID().toString()))));
        newPerson.addExtension(new Extension(EXT_STATUS, new StringType("APPROVED")));
        newPerson.addExtension(new Extension(EXT_APPROVED_DATE, new DateTimeType(new Date())));
        newPerson.addExtension(new Extension(EXT_KEYCLOAK_ID, new StringType(keycloakId)));
        newPerson.addExtension(new Extension(EXT_COUNTRY, new StringType("USA")));

        var outcome = fhirClientService.create(newPerson, getPracticeId());
        newPerson.setId(outcome.getId().getIdPart());

        return newPerson;
    }

    public ApiResponse<PortalLoginResponse> getProfile(Long userId) {
        try {
            String fhirId = String.valueOf(userId);
            Person person = fhirClientService.read(Person.class, fhirId, getPracticeId());

            if (person == null) {
                return ApiResponse.<PortalLoginResponse>builder()
                        .success(false)
                        .message("User not found")
                        .build();
            }

            PortalLoginResponse response = buildLoginResponse(person, fhirId);

            return ApiResponse.<PortalLoginResponse>builder()
                    .success(true)
                    .message("Profile retrieved successfully")
                    .data(response)
                    .build();

        } catch (Exception e) {
            return ApiResponse.<PortalLoginResponse>builder()
                    .success(false)
                    .message("User not found")
                    .build();
        }
    }

    // -------- Helper Methods --------

    private PortalLoginResponse buildLoginResponse(Person person, String fhirId) {
        PortalLoginResponse response = new PortalLoginResponse();
        response.setId((long) Math.abs(fhirId.hashCode()));
        response.setFhirId(fhirId);

        if (person.hasName()) {
            HumanName name = person.getNameFirstRep();
            response.setFirstName(name.getGivenAsSingleString());
            response.setLastName(name.getFamily());
        }

        response.setEmail(getEmail(person));
        response.setPhoneNumber(getPhone(person));
        response.setStatus(getStringExt(person, EXT_STATUS));
        response.setStreet(getStringExt(person, EXT_STREET));
        response.setCity(getStringExt(person, EXT_CITY));
        response.setState(getStringExt(person, EXT_STATE));
        response.setCountry(getStringExt(person, EXT_COUNTRY));
        response.setPostalCode(getStringExt(person, EXT_POSTAL_CODE));

        Extension dobExt = person.getExtensionByUrl(EXT_DOB);
        if (dobExt != null && dobExt.getValue() instanceof DateType) {
            Date date = ((DateType) dobExt.getValue()).getValue();
            if (date != null) {
                response.setDateOfBirth(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }
        
        // Set audit fields
        AuditDto audit = new AuditDto();
        audit.setCreatedDate(person.getMeta().getLastUpdated().toInstant());
        audit.setLastModifiedDate(person.getMeta().getLastUpdated().toInstant());
        response.setAudit(audit);

        return response;
    }

    private String getEmail(Person person) {
        return person.getTelecom().stream()
                .filter(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                .map(ContactPoint::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getPhone(Person person) {
        return person.getTelecom().stream()
                .filter(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.PHONE)
                .map(ContactPoint::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getStringExt(Person person, String url) {
        Extension ext = person.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
