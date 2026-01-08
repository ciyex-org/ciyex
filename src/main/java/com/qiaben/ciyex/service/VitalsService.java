package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Vitals Service - FHIR Only.
 * All vitals data is stored in HAPI FHIR server as Observation resources (vital-signs category).
 */
@Service
@Slf4j
public class VitalsService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final EncounterService encounterService;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // LOINC codes for vital signs
    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String LOINC_WEIGHT = "29463-7";
    private static final String LOINC_HEIGHT = "8302-2";
    private static final String LOINC_BP_PANEL = "85354-9";
    private static final String LOINC_BP_SYSTOLIC = "8480-6";
    private static final String LOINC_BP_DIASTOLIC = "8462-4";
    private static final String LOINC_HEART_RATE = "8867-4";
    private static final String LOINC_RESPIRATORY_RATE = "9279-1";
    private static final String LOINC_TEMPERATURE = "8310-5";
    private static final String LOINC_OXYGEN_SAT = "2708-6";
    private static final String LOINC_BMI = "39156-5";

    @Autowired
    public VitalsService(FhirClientService fhirClientService, PracticeContextService practiceContextService,
                         EncounterService encounterService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
        this.encounterService = encounterService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    private void validatePathVariable(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " must be a positive number. Provided: " + value);
        }
    }
    
    private void validatePatientExists(Long patientId) {
        try {
            fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Patient ID is invalid. Patient not found: " + patientId);
        }
    }
    
    private void validateEncounterExists(Long encounterId) {
        try {
            fhirClientService.read(Encounter.class, String.valueOf(encounterId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Encounter ID is invalid. Encounter not found: " + encounterId);
        }
    }

    // ✅ Get all vitals for a patient
    public List<VitalsDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        validatePatientExists(patientId);
        return getVitalsByPatient(patientId);
    }

    // ✅ Create vitals in FHIR
    public VitalsDto create(Long patientId, Long encounterId, VitalsDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.info("Creating vitals in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Calculate BMI
        dto.setBmi(calculateBmi(dto.getWeightKg(), dto.getHeightCm()));
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Create FHIR Observation (vital-signs panel)
        Observation fhirObservation = toFhirObservation(dto);

        MethodOutcome outcome = fhirClientService.create(fhirObservation, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        Observation created = (Observation) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Observation (vitals) with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get vitals by ID
    public VitalsDto get(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        VitalsDto dto = getByFhirId(String.valueOf(id));
        dto.setId(id);
        return dto;
    }

    public VitalsDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Observation (vitals) with ID: {}", fhirId);
        try {
            Observation fhirObservation = fhirClientService.read(Observation.class, fhirId, getPracticeId());
            VitalsDto dto = toVitalsDto(fhirObservation);
            dto.setId(Long.parseLong(fhirId));
            return dto;
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vitals ID is invalid. Vitals not found: " + fhirId);
        }
    }

    // ✅ Get vitals by encounter
    public List<VitalsDto> getByEncounter(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Getting FHIR Observations (vitals) for encounter: {}", encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("encounter").hasId("Encounter/" + encounterId))
                .where(Observation.CATEGORY.exactly().systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "vital-signs"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractVitals(bundle);
    }

    // ✅ Update vitals in FHIR
    public VitalsDto update(Long patientId, Long encounterId, Long id, VitalsDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Observation (vitals) with ID: {}", fhirId);

        // Validate resource exists
        VitalsDto existing = getByFhirId(fhirId);

        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Check if vitals is signed - prevent modification
        if (Boolean.TRUE.equals(existing.getSigned())) {
            throw new IllegalStateException("Signed vitals are read-only.");
        }

        // Calculate BMI
        dto.setBmi(calculateBmi(dto.getWeightKg(), dto.getHeightCm()));
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        Observation fhirObservation = toFhirObservation(dto);
        fhirObservation.setId(fhirId);

        fhirClientService.update(fhirObservation, getPracticeId());

        return get(patientId, encounterId, id);
    }

    // ✅ Delete vitals from FHIR
    public void delete(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Observation (vitals) with ID: {}", fhirId);

        // Validate resource exists
        VitalsDto existing = getByFhirId(fhirId);

        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Check if vitals is signed - prevent deletion
        if (Boolean.TRUE.equals(existing.getSigned())) {
            throw new IllegalStateException("Signed vitals cannot be deleted.");
        }

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Observation (vitals) with ID: {}", fhirId);
    }

    // ✅ E-Sign vitals
    public VitalsDto eSign(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Observation (vitals) with ID: {}", fhirId);

        try {
            Observation fhirObservation = fhirClientService.read(Observation.class, fhirId, getPracticeId());
            // Mark as final (signed)
            fhirObservation.setStatus(Observation.ObservationStatus.FINAL);
            fhirClientService.update(fhirObservation, getPracticeId());
            return toVitalsDto(fhirObservation);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vitals ID is invalid. Vitals not found: " + fhirId);
        }
    }

    // ✅ Print vitals as PDF
    public byte[] print(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        VitalsDto vitals = get(patientId, encounterId, id);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Vitals Report");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Vitals ID:", vitals.getFhirId()); y -= 20;

                if (vitals.getWeightKg() != null) { draw(cs, x, y, "Weight (kg):", String.valueOf(vitals.getWeightKg())); y -= 16; }
                if (vitals.getHeightCm() != null) { draw(cs, x, y, "Height (cm):", String.valueOf(vitals.getHeightCm())); y -= 16; }
                if (vitals.getBpSystolic() != null && vitals.getBpDiastolic() != null) {
                    draw(cs, x, y, "Blood Pressure:", vitals.getBpSystolic() + "/" + vitals.getBpDiastolic() + " mmHg"); y -= 16;
                }
                if (vitals.getPulse() != null) { draw(cs, x, y, "Pulse:", String.valueOf(vitals.getPulse()) + " bpm"); y -= 16; }
                if (vitals.getRespiration() != null) { draw(cs, x, y, "Respiration:", String.valueOf(vitals.getRespiration()) + "/min"); y -= 16; }
                if (vitals.getTemperatureC() != null) { draw(cs, x, y, "Temperature (°C):", String.valueOf(vitals.getTemperatureC())); y -= 16; }
                if (vitals.getOxygenSaturation() != null) { draw(cs, x, y, "Oxygen Saturation:", String.valueOf(vitals.getOxygenSaturation()) + "%"); y -= 16; }
                if (vitals.getBmi() != null) { draw(cs, x, y, "BMI:", String.valueOf(vitals.getBmi())); y -= 16; }
                if (StringUtils.hasText(vitals.getNotes())) { draw(cs, x, y, "Notes:", vitals.getNotes()); y -= 16; }

                y -= 10;
                draw(cs, x, y, "Signed:", Boolean.TRUE.equals(vitals.getSigned()) ? "Yes" : "No");
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Vitals PDF", ex);
        }
    }

    // ✅ Get all vitals for a patient (across all encounters)
    public List<VitalsDto> getVitalsByPatient(Long patientId) {
        log.info("Getting vitals for patient {} in org {}", patientId, RequestContext.get().getOrgName());

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(Observation.CATEGORY.exactly().systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "vital-signs"))
                
                .returnBundle(Bundle.class)
                .execute();

        List<VitalsDto> vitals = extractVitals(bundle);
        log.info("Found {} vitals records for patient {}", vitals.size(), patientId);
        return vitals;
    }

    // ✅ Portal Method - Map portal user email to EHR patient ID
    public Long getEhrPatientIdFromPortalUserEmail(String email) {
        log.info("Looking up EHR patient ID for portal user email {}", email);

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        // Portal patient lookup removed - repository deleted
        log.debug("Portal patient lookup skipped - repository not available");
        return null;
    }

    // ✅ Get vitals for current portal user based on email
    public List<VitalsDto> getVitalsForPortalUser(String email) {
        try {
            log.info("Getting vitals for portal user with email: {}", email);

            if (email == null || email.trim().isEmpty()) {
                log.error("Email is null or empty");
                return null;
            }

            Long patientId = getEhrPatientIdFromPortalUserEmail(email);
            log.info("Found EHR patientId {} for portal user email {}", patientId, email);

            if (patientId == null) {
                log.error("No EHR patient ID found for portal user email: {}", email);
                return null;
            }

            List<VitalsDto> vitals = getVitalsByPatient(patientId);
            log.info("Retrieved {} vitals records for patient {}", vitals != null ? vitals.size() : 0, patientId);

            return vitals;

        } catch (Exception e) {
            log.error("Error getting vitals for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get vitals for user", e);
        }
    }



    // ========== FHIR Mapping Methods ==========

    private Observation toFhirObservation(VitalsDto dto) {
        Observation observation = new Observation();

        // Category: vital-signs
        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital Signs");

        // Status
        observation.setStatus(Boolean.TRUE.equals(dto.getSigned()) 
                ? Observation.ObservationStatus.FINAL 
                : Observation.ObservationStatus.PRELIMINARY);

        // Code: Vital signs panel
        observation.getCode()
                .addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode("85353-1")
                .setDisplay("Vital signs, weight, height, head circumference, oxygen saturation and BMI panel");

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            observation.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter
        if (dto.getEncounterId() != null) {
            observation.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Effective date
        if (dto.getRecordedAt() != null) {
            observation.setEffective(new DateTimeType(Date.from(dto.getRecordedAt().atZone(ZoneId.systemDefault()).toInstant())));
        } else {
            observation.setEffective(new DateTimeType(new Date()));
        }

        // Components for each vital sign
        if (dto.getWeightKg() != null) {
            observation.addComponent()
                    .getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_WEIGHT).setDisplay("Body weight");
            observation.getComponentFirstRep().setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getWeightKg()))
                    .setUnit("kg")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("kg"));
        }

        if (dto.getHeightCm() != null) {
            Observation.ObservationComponentComponent heightComp = observation.addComponent();
            heightComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_HEIGHT).setDisplay("Body height");
            heightComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getHeightCm()))
                    .setUnit("cm")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("cm"));
        }

        if (dto.getBpSystolic() != null) {
            Observation.ObservationComponentComponent bpSysComp = observation.addComponent();
            bpSysComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_BP_SYSTOLIC).setDisplay("Systolic blood pressure");
            bpSysComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getBpSystolic()))
                    .setUnit("mmHg")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("mm[Hg]"));
        }

        if (dto.getBpDiastolic() != null) {
            Observation.ObservationComponentComponent bpDiaComp = observation.addComponent();
            bpDiaComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_BP_DIASTOLIC).setDisplay("Diastolic blood pressure");
            bpDiaComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getBpDiastolic()))
                    .setUnit("mmHg")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("mm[Hg]"));
        }

        if (dto.getPulse() != null) {
            Observation.ObservationComponentComponent pulseComp = observation.addComponent();
            pulseComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_HEART_RATE).setDisplay("Heart rate");
            pulseComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getPulse()))
                    .setUnit("/min")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("/min"));
        }

        if (dto.getRespiration() != null) {
            Observation.ObservationComponentComponent respComp = observation.addComponent();
            respComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_RESPIRATORY_RATE).setDisplay("Respiratory rate");
            respComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getRespiration()))
                    .setUnit("/min")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("/min"));
        }

        if (dto.getTemperatureC() != null) {
            Observation.ObservationComponentComponent tempComp = observation.addComponent();
            tempComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_TEMPERATURE).setDisplay("Body temperature");
            tempComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getTemperatureC()))
                    .setUnit("Cel")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("Cel"));
        }

        if (dto.getOxygenSaturation() != null) {
            Observation.ObservationComponentComponent o2Comp = observation.addComponent();
            o2Comp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_OXYGEN_SAT).setDisplay("Oxygen saturation");
            o2Comp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getOxygenSaturation()))
                    .setUnit("%")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("%"));
        }

        if (dto.getBmi() != null) {
            Observation.ObservationComponentComponent bmiComp = observation.addComponent();
            bmiComp.getCode().addCoding().setSystem(LOINC_SYSTEM).setCode(LOINC_BMI).setDisplay("Body mass index");
            bmiComp.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(dto.getBmi()))
                    .setUnit("kg/m2")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("kg/m2"));
        }

        // Notes
        if (dto.getNotes() != null) {
            observation.addNote().setText(dto.getNotes());
        }

        return observation;
    }

    private VitalsDto toVitalsDto(Observation observation) {
        VitalsDto dto = VitalsDto.builder().build();

        // FHIR ID
        if (observation.hasId()) {
            String fhirId = observation.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        // Signed status (FINAL = signed)
        dto.setSigned(observation.getStatus() == Observation.ObservationStatus.FINAL);

        // Subject (Patient)
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            String ref = observation.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Encounter
        if (observation.hasEncounter() && observation.getEncounter().hasReference()) {
            String ref = observation.getEncounter().getReference();
            if (ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring(10)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Effective date
        if (observation.hasEffectiveDateTimeType()) {
            dto.setRecordedAt(observation.getEffectiveDateTimeType().getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        // Extract components
        for (Observation.ObservationComponentComponent comp : observation.getComponent()) {
            if (!comp.hasCode() || !comp.getCode().hasCoding()) continue;
            String code = comp.getCode().getCodingFirstRep().getCode();
            if (!comp.hasValueQuantity()) continue;
            double value = comp.getValueQuantity().getValue().doubleValue();

            switch (code) {
                case LOINC_WEIGHT -> dto.setWeightKg(value);
                case LOINC_HEIGHT -> dto.setHeightCm(value);
                case LOINC_BP_SYSTOLIC -> dto.setBpSystolic(value);
                case LOINC_BP_DIASTOLIC -> dto.setBpDiastolic(value);
                case LOINC_HEART_RATE -> dto.setPulse(value);
                case LOINC_RESPIRATORY_RATE -> dto.setRespiration(value);
                case LOINC_TEMPERATURE -> dto.setTemperatureC(value);
                case LOINC_OXYGEN_SAT -> dto.setOxygenSaturation(value);
                case LOINC_BMI -> dto.setBmi(value);
            }
        }

        // Notes
        if (observation.hasNote()) {
            dto.setNotes(observation.getNoteFirstRep().getText());
        }
        
        if (observation.hasMeta()) {
            populateAudit(dto, observation.getMeta());
        }

        return dto;
    }

    private List<VitalsDto> extractVitals(Bundle bundle) {
        List<VitalsDto> vitals = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    vitals.add(toVitalsDto((Observation) entry.getResource()));
                }
            }
        }
        return vitals;
    }

    private Double calculateBmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || heightCm == 0) {
            return null;
        }
        double heightM = heightCm / 100.0;
        return Math.round((weightKg / (heightM * heightM)) * 10.0) / 10.0;
    }

    // --- PDF helpers
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }
    
    private void populateAudit(VitalsDto dto, Meta meta) {
        VitalsDto.Audit audit = new VitalsDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
