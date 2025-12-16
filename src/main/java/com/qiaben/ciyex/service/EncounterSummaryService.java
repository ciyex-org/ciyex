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
                            .section1(d.getSection1())
                            .section2(d.getSection2())
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
        sb.append("body { font-family: Arial, sans-serif; font-size: 9px; line-height: 1.2; padding: 15px; color: #000; }");
        sb.append(".header { border-bottom: 2px solid #000; padding-bottom: 5px; margin-bottom: 8px; }");
        sb.append(".company { font-size: 14px; font-weight: bold; }");
        sb.append(".info { font-size: 8px; color: #333; }");
        sb.append(".section { margin: 6px 0; page-break-inside: avoid; }");
        sb.append(".title { font-size: 10px; font-weight: bold; background: #e0e0e0; padding: 2px 4px; margin-bottom: 3px; }");
        sb.append(".row { display: flex; padding: 1px 0; }");
        sb.append(".label { font-weight: 600; min-width: 80px; font-size: 8px; }");
        sb.append(".value { flex: 1; font-size: 8px; }");
        sb.append(".grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; font-size: 8px; }");
        sb.append(".item { padding: 2px; border-left: 2px solid #ccc; }");
        sb.append(".compact { display: inline-block; margin-right: 10px; font-size: 8px; }");
        sb.append("@page { size: A4; margin: 10mm; }");
        sb.append("@media print { body { padding: 0; } }");
        sb.append("</style>");
        sb.append("</head><body>");
        
        // Header
        sb.append("<div class='header'>");
        sb.append("<div class='company'>Ciyex Health Solutions</div>");
        sb.append("<div class='info'>123 Main St, Chennai | +91 98765 43210 | info@ciyex.com</div>");
        sb.append("</div>");
        
        // Encounter Meta - compact
        if (dto.getMeta() != null) {
            sb.append("<div class='section'><div class='title'>ENCOUNTER</div><div class='grid'>");
            if (dto.getMeta().getVisitCategory() != null) sb.append("<div><b>Visit:</b> ").append(escape(dto.getMeta().getVisitCategory())).append("</div>");
            if (dto.getMeta().getType() != null) sb.append("<div><b>Type:</b> ").append(escape(dto.getMeta().getType())).append("</div>");
            if (dto.getMeta().getFacility() != null) sb.append("<div><b>Facility:</b> ").append(escape(dto.getMeta().getFacility())).append("</div>");
            if (dto.getMeta().getDateOfService() != null) sb.append("<div><b>Date:</b> ").append(escape(dto.getMeta().getDateOfService())).append("</div>");
            if (dto.getMeta().getReasonForVisit() != null) sb.append("<div><b>Reason:</b> ").append(escape(dto.getMeta().getReasonForVisit())).append("</div>");
            sb.append("</div></div>");
        }
        
        // Assigned Providers - inline
        if (dto.getAssignedProviders() != null && !dto.getAssignedProviders().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PROVIDERS</div>");
            for (var p : dto.getAssignedProviders()) {
                sb.append("<span class='compact'><b>").append(escape(p.getProviderName() != null ? p.getProviderName() : p.getName()));
                if (p.getRole() != null) sb.append("</b> (").append(escape(p.getRole())).append(")");
                else sb.append("</b>");
                sb.append("</span>");
            }
            sb.append("</div>");
        }
        
        // Chief Complaints - inline
        if (dto.getChiefComplaints() != null && !dto.getChiefComplaints().isEmpty()) {
            sb.append("<div class='section'><div class='title'>CHIEF COMPLAINT</div>");
            for (var cc : dto.getChiefComplaints()) {
                sb.append("<b>").append(escape(cc.getComplaint())).append("</b>");
                if (cc.getNotes() != null) sb.append(": ").append(escape(cc.getNotes()));
                sb.append(" ");
            }
            sb.append("</div>");
        }
        
        // Vitals - grid
        if (dto.getVitals() != null && !dto.getVitals().isEmpty()) {
            sb.append("<div class='section'><div class='title'>VITALS</div><div class='grid'>");
            for (var v : dto.getVitals()) {
                if (v.getWeightKg() != null) sb.append("<div>Wt: ").append(v.getWeightKg()).append("kg</div>");
                if (v.getHeightCm() != null) sb.append("<div>Ht: ").append(v.getHeightCm()).append("cm</div>");
                if (v.getBpSystolic() != null && v.getBpDiastolic() != null) 
                    sb.append("<div>BP: ").append(v.getBpSystolic()).append("/").append(v.getBpDiastolic()).append("</div>");
                if (v.getPulse() != null) sb.append("<div>Pulse: ").append(v.getPulse()).append("</div>");
                if (v.getTemperatureC() != null) sb.append("<div>Temp: ").append(v.getTemperatureC()).append("°C</div>");
                if (v.getOxygenSaturation() != null) sb.append("<div>O2: ").append(v.getOxygenSaturation()).append("%</div>");
                if (v.getBmi() != null) sb.append("<div>BMI: ").append(v.getBmi()).append("</div>");
            }
            sb.append("</div></div>");
        }
        
        // HPI - compact
        if (dto.getHpi() != null && !dto.getHpi().isEmpty()) {
            sb.append("<div class='section'><div class='title'>HPI</div>");
            for (var h : dto.getHpi()) {
                sb.append(escape(h.getDescription())).append(" ");
            }
            sb.append("</div>");
        }
        
        // Patient MH - inline
        if (dto.getPatientMH() != null && !dto.getPatientMH().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PATIENT MH</div>");
            for (var pmh : dto.getPatientMH()) {
                if (pmh.getDescription() != null) sb.append(escape(pmh.getDescription())).append("; ");
            }
            sb.append("</div>");
        }
        
        // PMH - inline
        if (dto.getPmh() != null && !dto.getPmh().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PMH</div>");
            for (var pmh : dto.getPmh()) {
                sb.append(escape(pmh.getDescription())).append("; ");
            }
            sb.append("</div>");
        }
        
        // Family History - inline
        if (dto.getFamilyHistory() != null && !dto.getFamilyHistory().isEmpty()) {
            sb.append("<div class='section'><div class='title'>FAMILY HISTORY</div>");
            for (var fh : dto.getFamilyHistory()) {
                if (fh.getRelation() != null) sb.append(escape(fh.getRelation())).append(": ");
                if (fh.getCondition() != null) sb.append(escape(fh.getCondition())).append("; ");
            }
            sb.append("</div>");
        }
        
        // Social History - inline
        if (dto.getSocialHistory() != null && dto.getSocialHistory().getEntries() != null && !dto.getSocialHistory().getEntries().isEmpty()) {
            sb.append("<div class='section'><div class='title'>SOCIAL HISTORY</div>");
            for (var sh : dto.getSocialHistory().getEntries()) {
                sb.append("<b>").append(escape(sh.getCategory())).append(":</b> ");
                if (sh.getValue() != null) sb.append(escape(sh.getValue())).append("; ");
            }
            sb.append("</div>");
        }
        
        // ROS - grid
        if (dto.getRos() != null && !dto.getRos().isEmpty()) {
            sb.append("<div class='section'><div class='title'>ROS</div><div class='grid'>");
            for (var ros : dto.getRos()) {
                sb.append("<div><b>").append(escape(ros.getSystemName())).append(":</b> ");
                sb.append(ros.getIsNegative() ? "(-)" : "(+)");
                if (ros.getFinding() != null) sb.append(" ").append(escape(ros.getFinding()));
                sb.append("</div>");
            }
            sb.append("</div></div>");
        }
        
        // Physical Exam - compact text format
        if (dto.getPhysicalExam() != null && !dto.getPhysicalExam().isEmpty()) {
            sb.append("<div class='section'><div class='title'>EXAM</div>");
            for (var pe : dto.getPhysicalExam()) {
                if (pe.getSections() != null) {
                    for (var sec : pe.getSections()) {
                        sb.append("<b>").append(escape(sec.getSectionKey())).append(":</b> ");
                        if (sec.getAllNormal()) sb.append("normal");
                        if (sec.getNormalText() != null) sb.append(escape(sec.getNormalText()));
                        if (sec.getFindings() != null) sb.append(" ").append(escape(sec.getFindings()));
                        sb.append(". ");
                    }
                }
            }
            sb.append("</div>");
        }
        
        // Procedures - inline
        if (dto.getProcedures() != null && !dto.getProcedures().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PROCEDURES</div>");
            for (var proc : dto.getProcedures()) {
                if (proc.getCpt4() != null) sb.append("<b>").append(escape(proc.getCpt4())).append("</b> ");
                if (proc.getDescription() != null) sb.append(escape(proc.getDescription())).append("; ");
            }
            sb.append("</div>");
        }
        
        // Assessment - compact
        if (dto.getAssessment() != null && !dto.getAssessment().isEmpty()) {
            sb.append("<div class='section'><div class='title'>ASSESSMENT</div>");
            int idx = 1;
            for (var a : dto.getAssessment()) {
                sb.append(idx++).append(". ").append(escape(a.getAssessment())).append(" ");
            }
            sb.append("</div>");
        }
        
        // Plan - compact
        if (dto.getPlan() != null && !dto.getPlan().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PLAN</div>");
            for (var p : dto.getPlan()) {
                if (p.getDiagnosticPlan() != null) sb.append("<b>Dx:</b> ").append(escape(p.getDiagnosticPlan())).append(" ");
                if (p.getPlan() != null) sb.append("<b>Plan:</b> ").append(escape(p.getPlan())).append(" ");
                if (p.getFollowUpVisit() != null) sb.append("<b>F/U:</b> ").append(escape(String.valueOf(p.getFollowUpVisit()))).append(" ");
            }
            sb.append("</div>");
        }
        
        // Provider Notes (SOAP) - compact
        if (dto.getProviderNotes() != null && !dto.getProviderNotes().isEmpty()) {
            sb.append("<div class='section'><div class='title'>SOAP</div>");
            for (var note : dto.getProviderNotes()) {
                if (note.getSubjective() != null) sb.append("<b>S:</b> ").append(escape(note.getSubjective())).append(" ");
                if (note.getObjective() != null) sb.append("<b>O:</b> ").append(escape(note.getObjective())).append(" ");
                if (note.getAssessment() != null) sb.append("<b>A:</b> ").append(escape(note.getAssessment())).append(" ");
                if (note.getPlan() != null) sb.append("<b>P:</b> ").append(escape(note.getPlan())).append(" ");
            }
            sb.append("</div>");
        }
        
        // Provider Signature - minimal
        if (dto.getProviderSignature() != null && dto.getProviderSignature().getSignedBy() != null) {
            sb.append("<div class='section'><div class='title'>SIGNATURE</div>");
            sb.append("<b>Signed:</b> ").append(escape(dto.getProviderSignature().getSignedBy()));
            if (dto.getProviderSignature().getSignedAt() != null) 
                sb.append(" at ").append(escape(dto.getProviderSignature().getSignedAt()));
            sb.append("</div>");
        }
        
        // Date/Time Finalized - minimal
        if (dto.getDateTimeFinalized() != null && dto.getDateTimeFinalized().getFinalizedAt() != null) {
            sb.append("<div class='section'><b>Finalized:</b> ").append(escape(dto.getDateTimeFinalized().getFinalizedAt())).append("</div>");
        }
        
        sb.append("</body></html>");
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



    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#039;");
    }
}
