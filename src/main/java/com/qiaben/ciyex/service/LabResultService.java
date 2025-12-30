package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.LabResultDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LabResult Service - FHIR Only.
 * All lab result data is stored in HAPI FHIR server as DiagnosticReport/Observation resources.
 */
@Service
@Slf4j
public class LabResultService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public LabResultService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Get one lab result
    public LabResultDto getOne(Long id) {
        String fhirId = String.valueOf(id);
        try {
            DiagnosticReport report = fhirClientService.read(DiagnosticReport.class, fhirId, getPracticeId());
            return toLabResultDto(report);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    // ✅ Get all lab results
    public List<LabResultDto> getAll() {
        log.debug("Getting all FHIR DiagnosticReports (lab results)");

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(DiagnosticReport.class)
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractLabResults(bundle);
    }

    // ✅ Get lab results for patient
    public List<LabResultDto> getForPatient(Long patientId) {
        log.debug("Getting FHIR DiagnosticReports for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(DiagnosticReport.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractLabResults(bundle);
    }

    // ✅ Create lab result
    public LabResultDto create(LabResultDto dto) {
        log.info("Creating lab result in FHIR for patient: {}", dto.getPatientId());

        DiagnosticReport report = toFhirDiagnosticReport(dto);
        MethodOutcome outcome = fhirClientService.create(report, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setExternalId(fhirId);
        log.info("Created FHIR DiagnosticReport (lab result) with ID: {}", fhirId);
        return dto;
    }

    // ✅ Update lab result
    public LabResultDto update(Long id, LabResultDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR DiagnosticReport (lab result) with ID: {}", fhirId);

        DiagnosticReport report = toFhirDiagnosticReport(dto);
        report.setId(fhirId);

        fhirClientService.update(report, getPracticeId());

        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete lab result
    public void delete(Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR DiagnosticReport (lab result) with ID: {}", fhirId);
        fhirClientService.delete(DiagnosticReport.class, fhirId, getPracticeId());
    }

    // ✅ Search lab results
    public List<LabResultDto> search(String q) {
        // For FHIR, we search by code or name
        log.debug("Searching FHIR DiagnosticReports with query: {}", q);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(DiagnosticReport.class)
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        String qq = (q == null ? "" : q).trim().toLowerCase();
        return extractLabResults(bundle).stream()
                .filter(dto -> {
                    String code = safe(dto.getCode());
                    String testName = safe(dto.getTestName());
                    String specimen = safe(dto.getSpecimen());
                    String status = safe(dto.getStatus());
                    String value = safe(dto.getValue());
                    return code.contains(qq) || testName.contains(qq) || specimen.contains(qq) ||
                           status.contains(qq) || value.contains(qq);
                })
                .collect(Collectors.toList());
    }

    // ========== FHIR Mapping Methods ==========

    private DiagnosticReport toFhirDiagnosticReport(LabResultDto dto) {
        DiagnosticReport report = new DiagnosticReport();

        // Category: LAB
        report.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0074")
                .setCode("LAB")
                .setDisplay("Laboratory");

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            report.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Status
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

        // Code (test code/name)
        if (dto.getCode() != null || dto.getTestName() != null) {
            report.setCode(new CodeableConcept()
                    .addCoding(new Coding().setCode(dto.getCode()))
                    .setText(dto.getTestName()));
        }

        // Effective date (result date)
        if (dto.getResultDate() != null) {
            report.setEffective(new DateTimeType(dto.getResultDate()));
        }

        // Issued date (reported date)
        if (dto.getReportedDate() != null) {
            try {
                report.setIssued(java.sql.Date.valueOf(dto.getReportedDate()));
            } catch (Exception e) {
                // Invalid date format
            }
        }

        // Specimen
        if (dto.getSpecimen() != null) {
            report.addSpecimen(new Reference().setDisplay(dto.getSpecimen()));
        }

        // Conclusion (recommendations)
        if (dto.getRecommendations() != null) {
            report.setConclusion(dto.getRecommendations());
        }

        return report;
    }

    private LabResultDto toLabResultDto(DiagnosticReport report) {
        LabResultDto dto = new LabResultDto();

        // FHIR ID
        if (report.hasId()) {
            dto.setExternalId(report.getIdElement().getIdPart());
        }

        // Patient ID
        if (report.hasSubject() && report.getSubject().hasReference()) {
            String ref = report.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Code and test name
        if (report.hasCode()) {
            if (report.getCode().hasCoding()) {
                dto.setCode(report.getCode().getCodingFirstRep().getCode());
            }
            dto.setTestName(report.getCode().getText());
        }

        // Status
        if (report.hasStatus()) {
            dto.setStatus(report.getStatus().toCode());
        }

        // Result date
        if (report.hasEffectiveDateTimeType()) {
            dto.setResultDate(report.getEffectiveDateTimeType().getValueAsString());
        }

        // Reported date
        if (report.hasIssued()) {
            dto.setReportedDate(report.getIssued().toString());
        }

        // Specimen
        if (report.hasSpecimen()) {
            dto.setSpecimen(report.getSpecimenFirstRep().getDisplay());
        }

        // Recommendations
        if (report.hasConclusion()) {
            dto.setRecommendations(report.getConclusion());
        }

        return dto;
    }

    private List<LabResultDto> extractLabResults(Bundle bundle) {
        List<LabResultDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof DiagnosticReport) {
                    items.add(toLabResultDto((DiagnosticReport) entry.getResource()));
                }
            }
        }
        return items;
    }

    private String safe(String s) {
        return (s == null ? "" : s).toLowerCase();
    }
}
