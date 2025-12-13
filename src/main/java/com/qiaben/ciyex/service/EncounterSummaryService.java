package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EncounterSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncounterSummaryService {

    private final AssignedProviderService assignedProviderService;
    private final ChiefComplaintService chiefComplaintService;
    private final VitalsService vitalsService;
    private final HistoryOfPresentIllnessService hpiService;
    private final PastMedicalHistoryService pmhService;
    private final PatientMedicalHistoryService patientMHService;
    private final FamilyHistoryService familyHistoryService;
    private final SocialHistoryService socialHistoryService;
    private final ReviewOfSystemService rosService;
    private final PhysicalExamService physicalExamService;
    private final ProcedureService procedureService;
    private final AssessmentService assessmentService;
    private final PlanService planService;
    private final ProviderNoteService providerNoteService;
    private final ProviderSignatureService providerSignatureService;
    private final DateTimeFinalizedService dateTimeFinalizedService;
    private final com.qiaben.ciyex.repository.EncounterRepository encounterRepository;
    private final com.qiaben.ciyex.repository.ProviderRepository providerRepository;

    public EncounterSummaryDto load(Long patientId, Long encounterId) {
        var encounter = encounterRepository.findByIdAndPatientId(encounterId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Encounter not found"));

        EncounterSummaryDto.EncounterMeta meta = EncounterSummaryDto.EncounterMeta.builder()
                .visitCategory(encounter.getVisitCategory())
                .type(encounter.getType())
                .facility(encounter.getEncounterProvider())
                .dateOfService(encounter.getEncounterDate() != null ? encounter.getEncounterDate().toString() : null)
                .reasonForVisit(encounter.getReasonForVisit())
                .build();

        return EncounterSummaryDto.builder()
                .meta(meta)
                .assignedProviders(mapAssignedProviders(patientId, encounterId))
                .chiefComplaints(mapChiefComplaints(patientId, encounterId))
                .vitals(mapVitals(patientId, encounterId))
                .hpi(mapHPI(patientId, encounterId))
                .pmh(mapPMH(patientId, encounterId))
                .patientMH(mapPatientMH(patientId, encounterId))
                .familyHistory(mapFamilyHistory(patientId, encounterId))
                .socialHistory(mapSocialHistory(patientId, encounterId))
                .ros(mapROS(patientId, encounterId))
                .physicalExam(mapPhysicalExam(patientId, encounterId))
                .procedures(mapProcedures(patientId, encounterId))
                .assessment(mapAssessment(patientId, encounterId))
                .plan(mapPlan(patientId, encounterId))
                .providerNotes(mapProviderNotes(patientId, encounterId))
                .providerSignature(mapProviderSignature(patientId, encounterId))
                .dateTimeFinalized(mapDateTimeFinalized(patientId, encounterId))
                .build();
    }

    private List<EncounterSummaryDto.AssignedProvider> mapAssignedProviders(Long patientId, Long encounterId) {
        try {
            return assignedProviderService.list(patientId, encounterId).stream()
                    .map(d -> {
                        String providerName = null;
                        if (d.getProviderId() != null) {
                            try {
                                var providerOpt = providerRepository.findById(d.getProviderId());
                                if (providerOpt.isPresent()) {
                                    var provider = providerOpt.get();
                                    providerName = provider.getFirstName() + " " + provider.getLastName();
                                }
                            } catch (Exception ex) {
                                log.debug("Could not fetch provider name for ID: {}", d.getProviderId());
                            }
                        }
                        
                        return EncounterSummaryDto.AssignedProvider.builder()
                                .id(d.getProviderId())
                                .providerName(providerName)
                                .name(providerName)
                                .role(d.getRole())
                                .start(d.getStartDate())
                                .end(d.getEndDate())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping assigned providers", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.ChiefComplaint> mapChiefComplaints(Long patientId, Long encounterId) {
        try {
            return chiefComplaintService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.ChiefComplaint.builder()
                            .id(d.getId())
                            .complaint(d.getComplaint())
                            .notes(d.getDetails())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping chief complaints", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.Vitals> mapVitals(Long patientId, Long encounterId) {
        try {
            return vitalsService.getByEncounter(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.Vitals.builder()
                            .id(d.getId())
                            .weightKg(d.getWeightKg())
                            .weightLbs(d.getWeightLbs())
                            .heightCm(d.getHeightCm())
                            .heightIn(d.getHeightIn())
                            .bpSystolic(d.getBpSystolic() != null ? d.getBpSystolic().intValue() : null)
                            .bpDiastolic(d.getBpDiastolic() != null ? d.getBpDiastolic().intValue() : null)
                            .pulse(d.getPulse() != null ? d.getPulse().intValue() : null)
                            .respiration(d.getRespiration() != null ? d.getRespiration().intValue() : null)
                            .temperatureC(d.getTemperatureC())
                            .temperatureF(d.getTemperatureF())
                            .oxygenSaturation(d.getOxygenSaturation())
                            .bmi(d.getBmi())
                            .notes(d.getNotes())
                            .recordedAt(d.getRecordedAt() != null ? d.getRecordedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping vitals", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.HPIEntry> mapHPI(Long patientId, Long encounterId) {
        try {
            return hpiService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.HPIEntry.builder()
                            .id(d.getId())
                            .description(d.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping HPI", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.PMHEntry> mapPMH(Long patientId, Long encounterId) {
        try {
            return pmhService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.PMHEntry.builder()
                            .id(d.getId())
                            .description(d.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping PMH", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.PatientMHEntry> mapPatientMH(Long patientId, Long encounterId) {
        try {
            return patientMHService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.PatientMHEntry.builder()
                            .id(d.getId())
                            .description(d.getConditionName() != null ? d.getConditionName() : d.getMedicalCondition())
                            .text(d.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping patient MH", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.FamilyHistory> mapFamilyHistory(Long patientId, Long encounterId) {
        try {
            return familyHistoryService.list(patientId, encounterId).stream()
                    .flatMap(fh -> fh.getEntries().stream()
                            .map(e -> EncounterSummaryDto.FamilyHistory.builder()
                                    .id(fh.getId())
                                    .relation(e.getRelation())
                                    .condition(e.getDiagnosisText())
                                    .details(e.getNotes())
                                    .build()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping family history", e);
            return List.of();
        }
    }

    private EncounterSummaryDto.SocialHistory mapSocialHistory(Long patientId, Long encounterId) {
        try {
            var list = socialHistoryService.getAllByPatient(patientId);
            if (list == null || list.isEmpty()) return null;
            var sh = list.get(0);
            if (sh.getEntries() == null) return null;
            return EncounterSummaryDto.SocialHistory.builder()
                    .entries(sh.getEntries().stream()
                            .map(e -> EncounterSummaryDto.SocialHistoryEntry.builder()
                                    .id(e.getId())
                                    .category(e.getCategory())
                                    .value(e.getValue())
                                    .details(e.getDetails())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        } catch (Exception e) {
            log.error("Error mapping social history", e);
            return null;
        }
    }

    private List<EncounterSummaryDto.ROSEntry> mapROS(Long patientId, Long encounterId) {
        try {
            return rosService.list(patientId, encounterId).stream()
                    .map(d -> {
                        var builder = EncounterSummaryDto.ROSEntry.builder()
                                .id(d.getId())
                                .systemName(d.getSystemName())
                                .isNegative(d.getIsNegative())
                                .notes(d.getNotes());
                        
                        // Map systemDetails to finding if available
                        if (d.getSystemDetails() != null && !d.getSystemDetails().isEmpty()) {
                            builder.finding(String.join(", ", d.getSystemDetails()));
                        }
                        
                        return builder.build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping ROS", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.PhysicalExam> mapPhysicalExam(Long patientId, Long encounterId) {
        try {
            return physicalExamService.list(patientId, encounterId).stream()
                    .map(pe -> EncounterSummaryDto.PhysicalExam.builder()
                            .id(pe.getId())
                            .summary(pe.getSummary())
                            .sections(pe.getSections().stream()
                                    .map(s -> EncounterSummaryDto.PhysicalExamSection.builder()
                                            .sectionKey(s.getSectionKey())
                                            .allNormal(s.getAllNormal())
                                            .normalText(s.getNormalText())
                                            .findings(s.getFindings())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping physical exam", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.Procedure> mapProcedures(Long patientId, Long encounterId) {
        try {
            return procedureService.getAllByEncounter(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.Procedure.builder()
                            .id(d.getId())
                            .cpt4(d.getCpt4())
                            .description(d.getDescription())
                            .procedureName(d.getNote())
                            .units(d.getUnits())
                            .rate(d.getRate() != null ? Double.valueOf(d.getRate()) : null)
                            .relatedIcds(d.getRelatedIcds())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping procedures", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.Assessment> mapAssessment(Long patientId, Long encounterId) {
        try {
            return assessmentService.getAllByEncounter(patientId, encounterId).stream()
                    .map(d -> {
                        var builder = EncounterSummaryDto.Assessment.builder()
                                .id(d.getId())
                                .assessment(d.getAssessmentText());
                        
                        // Add text field if it exists in DTO
                        if (d.getAssessmentText() != null) {
                            builder.text(d.getAssessmentText());
                        }
                        
                        return builder.build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping assessment", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.Plan> mapPlan(Long patientId, Long encounterId) {
        try {
            return planService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.Plan.builder()
                            .id(d.getId())
                            .plan(d.getPlan())
                            .diagnosticPlan(d.getDiagnosticPlan())
                            .notes(d.getNotes())
                            .sectionsJson(d.getSectionsJson())
                            .followUpVisit(d.getFollowUpVisit())
                            .returnWorkSchool(d.getReturnWorkSchool())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping plan", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.ProviderNote> mapProviderNotes(Long patientId, Long encounterId) {
        try {
            return providerNoteService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.ProviderNote.builder()
                            .id(d.getId())
                            .subjective(d.getSubjective())
                            .objective(d.getObjective())
                            .assessment(d.getAssessment())
                            .plan(d.getPlan())
                            .narrative(d.getNarrative())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error mapping provider notes", e);
            return List.of();
        }
    }

    private EncounterSummaryDto.ProviderSignature mapProviderSignature(Long patientId, Long encounterId) {
        try {
            var list = providerSignatureService.list(patientId, encounterId);
            if (list.isEmpty()) return null;
            var sig = list.get(0);
            return EncounterSummaryDto.ProviderSignature.builder()
                    .signedBy(sig.getSignedBy())
                    .signedAt(sig.getSignedAt())
                    .status(sig.getStatus())
                    .signatureData(sig.getSignatureData())
                    .signatureFormat(sig.getSignatureFormat())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping provider signature", e);
            return null;
        }
    }

    private EncounterSummaryDto.DateTimeFinalized mapDateTimeFinalized(Long patientId, Long encounterId) {
        try {
            var list = dateTimeFinalizedService.list(patientId, encounterId);
            if (list.isEmpty()) return null;
            var dtf = list.get(0);
            return EncounterSummaryDto.DateTimeFinalized.builder()
                    .finalizedAt(dtf.getFinalizedAt())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping date time finalized", e);
            return null;
        }
    }

    public String buildHtml(EncounterSummaryDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
        sb.append("<style>");
        sb.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; background: #f9fafb; color: #1f2937; }");
        sb.append(".container { max-width: 900px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        sb.append(".header { border-bottom: 3px solid #3b82f6; padding-bottom: 20px; margin-bottom: 30px; }");
        sb.append(".company-title { font-size: 28px; font-weight: 700; color: #1e40af; margin-bottom: 8px; }");
        sb.append(".company-info { font-size: 14px; color: #6b7280; line-height: 1.6; }");
        sb.append(".section { margin: 30px 0; padding: 20px; background: #f9fafb; border-radius: 8px; border-left: 4px solid #3b82f6; }");
        sb.append(".section-title { font-size: 18px; font-weight: 700; color: #1e40af; margin-bottom: 15px; text-transform: uppercase; letter-spacing: 0.5px; }");
        sb.append(".card { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 15px; margin: 10px 0; }");
        sb.append(".row { display: flex; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }");
        sb.append(".row:last-child { border-bottom: none; }");
        sb.append(".label { font-weight: 600; color: #374151; min-width: 180px; }");
        sb.append(".value { color: #1f2937; flex: 1; }");
        sb.append(".list-item { padding: 10px; margin: 8px 0; background: white; border-radius: 6px; border-left: 3px solid #60a5fa; }");
        sb.append(".grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }");
        sb.append("@media print { body { padding: 20px; } .container { box-shadow: none; } }");
        sb.append("</style>");
        sb.append("</head><body><div class='container'>");
        
        sb.append("<div class='header'>");
        sb.append("<div class='company-title'>Ciyex Health Solutions</div>");
        sb.append("<div class='company-info'>");
        sb.append("123 Main Street, Chennai, India<br/>");
        sb.append("Phone: +91 98765 43210 | Email: info@ciyex.com");
        sb.append("</div></div>");
        
        if (dto.getMeta() != null) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Encounter Details</div>");
            sb.append("<div class='card'>");
            writeRowStyled(sb, "Visit Category", dto.getMeta().getVisitCategory());
            writeRowStyled(sb, "Type", dto.getMeta().getType());
            writeRowStyled(sb, "Facility", dto.getMeta().getFacility());
            writeRowStyled(sb, "Date of Service", dto.getMeta().getDateOfService());
            writeRowStyled(sb, "Reason for Visit", dto.getMeta().getReasonForVisit());
            sb.append("</div></div>");
        }
        
        appendProviders(sb, dto);
        appendChiefComplaints(sb, dto);
        appendVitals(sb, dto);
        appendHPI(sb, dto);
        appendPatientMH(sb, dto);
        appendPMH(sb, dto);
        appendFamilyHistory(sb, dto);
        appendSocialHistory(sb, dto);
        appendROS(sb, dto);
        appendPhysicalExam(sb, dto);
        appendProcedures(sb, dto);
        appendAssessment(sb, dto);
        appendPlan(sb, dto);
        appendProviderNotes(sb, dto);
        appendProviderSignature(sb, dto);
        appendDateTimeFinalized(sb, dto);
        
        sb.append("</div></body></html>");
        return sb.toString();
    }

    public byte[] renderPdfFromHtml(String html) {
        try (java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to render PDF", ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
    }

    private static void writeRowStyled(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return;
        sb.append("<div class='row'>");
        sb.append("<span class='label'>").append(escape(label)).append(":</span>");
        sb.append("<span class='value'>").append(escape(value)).append("</span>");
        sb.append("</div>");
    }

    private static void appendProviders(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getAssignedProviders() != null && !dto.getAssignedProviders().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Assigned Providers</div>");
            for (var p : dto.getAssignedProviders()) {
                sb.append("<div class='list-item'>");
                sb.append("<strong>").append(escape(p.getProviderName() != null ? p.getProviderName() : p.getName())).append("</strong>");
                if (p.getRole() != null) sb.append(" - ").append(escape(p.getRole()));
                if (p.getStart() != null || p.getEnd() != null) {
                    sb.append("<br/><small style='color:#6b7280;'>");
                    if (p.getStart() != null) sb.append("Start: ").append(escape(p.getStart()));
                    if (p.getEnd() != null) sb.append(" | End: ").append(escape(p.getEnd()));
                    sb.append("</small>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendChiefComplaints(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getChiefComplaints() != null && !dto.getChiefComplaints().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Chief Complaints</div>");
            for (var cc : dto.getChiefComplaints()) {
                sb.append("<div class='list-item'>");
                sb.append("<strong>").append(escape(cc.getComplaint())).append("</strong>");
                if (cc.getNotes() != null) sb.append("<br/>").append(escape(cc.getNotes()));
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendVitals(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getVitals() != null && !dto.getVitals().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Vitals</div>");
            for (var v : dto.getVitals()) {
                sb.append("<div class='card'><div class='grid'>");
                if (v.getWeightKg() != null) sb.append("<div><span class='label'>Weight:</span> ").append(v.getWeightKg()).append(" kg</div>");
                if (v.getHeightCm() != null) sb.append("<div><span class='label'>Height:</span> ").append(v.getHeightCm()).append(" cm</div>");
                if (v.getBpSystolic() != null && v.getBpDiastolic() != null) 
                    sb.append("<div><span class='label'>BP:</span> ").append(v.getBpSystolic()).append("/").append(v.getBpDiastolic()).append(" mmHg</div>");
                if (v.getPulse() != null) sb.append("<div><span class='label'>Pulse:</span> ").append(v.getPulse()).append(" bpm</div>");
                if (v.getTemperatureC() != null) sb.append("<div><span class='label'>Temperature:</span> ").append(v.getTemperatureC()).append(" °C</div>");
                if (v.getOxygenSaturation() != null) sb.append("<div><span class='label'>O2 Sat:</span> ").append(v.getOxygenSaturation()).append("%</div>");
                if (v.getBmi() != null) sb.append("<div><span class='label'>BMI:</span> ").append(v.getBmi()).append("</div>");
                sb.append("</div>");
                if (v.getNotes() != null) sb.append("<div style='margin-top:10px;'><strong>Notes:</strong> ").append(escape(v.getNotes())).append("</div>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendHPI(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getHpi() != null && !dto.getHpi().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>History of Present Illness</div>");
            for (var h : dto.getHpi()) {
                sb.append("<div class='list-item'>").append(escape(h.getDescription())).append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendPatientMH(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getPatientMH() != null && !dto.getPatientMH().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Patient Medical History</div>");
            for (var pmh : dto.getPatientMH()) {
                sb.append("<div class='list-item'>");
                if (pmh.getDescription() != null) sb.append(escape(pmh.getDescription()));
                if (pmh.getText() != null) sb.append("<br/>").append(escape(pmh.getText()));
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendPMH(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getPmh() != null && !dto.getPmh().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Past Medical History</div>");
            for (var pmh : dto.getPmh()) {
                sb.append("<div class='list-item'>").append(escape(pmh.getDescription())).append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendFamilyHistory(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getFamilyHistory() != null && !dto.getFamilyHistory().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Family History</div>");
            for (var fh : dto.getFamilyHistory()) {
                sb.append("<div class='list-item'>");
                if (fh.getRelation() != null) sb.append("<strong>").append(escape(fh.getRelation())).append(":</strong> ");
                if (fh.getCondition() != null) sb.append(escape(fh.getCondition()));
                if (fh.getDetails() != null) sb.append(" - ").append(escape(fh.getDetails()));
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendSocialHistory(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getSocialHistory() != null && dto.getSocialHistory().getEntries() != null && !dto.getSocialHistory().getEntries().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Social History</div>");
            for (var sh : dto.getSocialHistory().getEntries()) {
                sb.append("<div class='list-item'>");
                sb.append("<strong>").append(escape(sh.getCategory())).append(":</strong> ");
                if (sh.getValue() != null) sb.append(escape(sh.getValue()));
                if (sh.getDetails() != null) sb.append(" - ").append(escape(sh.getDetails()));
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendROS(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getRos() != null && !dto.getRos().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Review of Systems</div>");
            for (var ros : dto.getRos()) {
                sb.append("<div class='list-item'>");
                sb.append("<strong>").append(escape(ros.getSystemName())).append(":</strong> ");
                sb.append(ros.getIsNegative() ? "Negative" : "Positive");
                if (ros.getFinding() != null) sb.append(" - ").append(escape(ros.getFinding()));
                if (ros.getNotes() != null) sb.append("<br/>").append(escape(ros.getNotes()));
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendPhysicalExam(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getPhysicalExam() != null && !dto.getPhysicalExam().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Physical Examination</div>");
            for (var pe : dto.getPhysicalExam()) {
                if (pe.getSections() != null) {
                    for (var sec : pe.getSections()) {
                        if (sec.getNormalText() != null || sec.getFindings() != null) {
                            sb.append("<div class='list-item'>");
                            sb.append("<strong>").append(escape(sec.getSectionKey())).append(":</strong> ");
                            if (sec.getAllNormal()) sb.append("Normal");
                            if (sec.getNormalText() != null) sb.append(" - ").append(escape(sec.getNormalText()));
                            if (sec.getFindings() != null) sb.append("<br/>Findings: ").append(escape(sec.getFindings()));
                            sb.append("</div>");
                        }
                    }
                }
            }
            sb.append("</div>");
        }
    }

    private static void appendProcedures(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getProcedures() != null && !dto.getProcedures().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Procedures</div>");
            for (var proc : dto.getProcedures()) {
                sb.append("<div class='list-item'>");
                if (proc.getCpt4() != null) sb.append("<strong>").append(escape(proc.getCpt4())).append("</strong> - ");
                if (proc.getDescription() != null) sb.append(escape(proc.getDescription()));
                if (proc.getProcedureName() != null) sb.append("<br/>").append(escape(proc.getProcedureName()));
                if (proc.getUnits() != null) sb.append(" | Units: ").append(proc.getUnits());
                if (proc.getRate() != null) sb.append(" | Rate: $").append(proc.getRate());
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendAssessment(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getAssessment() != null && !dto.getAssessment().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Assessment</div>");
            for (var a : dto.getAssessment()) {
                sb.append("<div class='list-item'>").append(escape(a.getAssessment())).append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendPlan(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getPlan() != null && !dto.getPlan().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Plan</div>");
            for (var p : dto.getPlan()) {
                sb.append("<div class='card'>");
                if (p.getDiagnosticPlan() != null) sb.append("<div><strong>Diagnostic Plan:</strong> ").append(escape(p.getDiagnosticPlan())).append("</div>");
                if (p.getPlan() != null) sb.append("<div><strong>Plan:</strong> ").append(escape(p.getPlan())).append("</div>");
                if (p.getNotes() != null) sb.append("<div><strong>Notes:</strong> ").append(escape(p.getNotes())).append("</div>");
                if (p.getFollowUpVisit() != null) sb.append("<div><strong>Follow-up:</strong> ").append(escape(String.valueOf(p.getFollowUpVisit()))).append("</div>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendProviderNotes(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getProviderNotes() != null && !dto.getProviderNotes().isEmpty()) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Provider Notes (SOAP)</div>");
            for (var note : dto.getProviderNotes()) {
                sb.append("<div class='card'>");
                if (note.getSubjective() != null) sb.append("<div><strong>Subjective:</strong> ").append(escape(note.getSubjective())).append("</div>");
                if (note.getObjective() != null) sb.append("<div><strong>Objective:</strong> ").append(escape(note.getObjective())).append("</div>");
                if (note.getAssessment() != null) sb.append("<div><strong>Assessment:</strong> ").append(escape(note.getAssessment())).append("</div>");
                if (note.getPlan() != null) sb.append("<div><strong>Plan:</strong> ").append(escape(note.getPlan())).append("</div>");
                if (note.getNarrative() != null) sb.append("<div><strong>Narrative:</strong> ").append(escape(note.getNarrative())).append("</div>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
    }

    private static void appendProviderSignature(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getProviderSignature() != null) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Provider Signature</div>");
            sb.append("<div class='card'>");
            if (dto.getProviderSignature().getSignedBy() != null) 
                sb.append("<div><strong>Signed By:</strong> ").append(escape(dto.getProviderSignature().getSignedBy())).append("</div>");
            if (dto.getProviderSignature().getSignedAt() != null) 
                sb.append("<div><strong>Signed At:</strong> ").append(escape(dto.getProviderSignature().getSignedAt())).append("</div>");
            sb.append("</div></div>");
        }
    }

    private static void appendDateTimeFinalized(StringBuilder sb, EncounterSummaryDto dto) {
        if (dto.getDateTimeFinalized() != null && dto.getDateTimeFinalized().getFinalizedAt() != null) {
            sb.append("<div class='section'>");
            sb.append("<div class='section-title'>Finalization</div>");
            sb.append("<div class='card'>");
            sb.append("<div><strong>Finalized At:</strong> ").append(escape(dto.getDateTimeFinalized().getFinalizedAt())).append("</div>");
            sb.append("</div></div>");
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#039;");
    }
}
