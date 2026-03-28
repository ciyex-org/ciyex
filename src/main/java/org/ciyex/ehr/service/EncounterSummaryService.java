package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.EncounterSummaryDto;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncounterSummaryService {

    private final GenericFhirResourceService fhirService;
    private final ProviderNoteService providerNoteService;
    private final ProviderSignatureService providerSignatureService;
    private final DateTimeFinalizedService dateTimeFinalizedService;

    @SuppressWarnings("unchecked")
    public EncounterSummaryDto load(Long patientId, Long encounterId) {
        // Get encounter via generic FHIR service
        Map<String, Object> encounter = fhirService.get("encounters", patientId, String.valueOf(encounterId));
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter not found");
        }

        EncounterSummaryDto.EncounterMeta meta = EncounterSummaryDto.EncounterMeta.builder()
                .visitCategory(str(encounter, "visitCategory"))
                .type(str(encounter, "type", "appointmentType"))
                .facility(str(encounter, "encounterProvider", "provider"))
                .dateOfService(str(encounter, "encounterDate", "date"))
                .reasonForVisit(str(encounter, "reasonForVisit"))
                .build();

        // Load encounter-form composition data (stores all dynamic form sections)
        Map<String, Object> formData = loadEncounterFormData(patientId, encounterId);

        // Also load from individual FHIR tabs as fallback for missing sections
        Map<String, Object> mergedData = new java.util.LinkedHashMap<>(formData);
        loadTabFallback(mergedData, patientId, encounterId, "chief-complaint", "cc", "chiefComplaint");
        loadTabFallback(mergedData, patientId, encounterId, "hpi", "hpi", "historyOfPresentIllness");
        loadTabFallback(mergedData, patientId, encounterId, "pmh", "pmh", "pastMedicalHistory");
        loadTabFallback(mergedData, patientId, encounterId, "family-history", "fh", "familyHistory");
        loadTabFallback(mergedData, patientId, encounterId, "social-history", "socialHistory", "sh");
        loadTabFallback(mergedData, patientId, encounterId, "ros", "ros", "reviewOfSystems");
        loadTabFallback(mergedData, patientId, encounterId, "physical-exam", "pe", "physicalExam");
        loadTabFallback(mergedData, patientId, encounterId, "assessment", "assessment", "assessmentText");
        loadTabFallback(mergedData, patientId, encounterId, "plan", "plan", "planText");
        loadTabFallback(mergedData, patientId, encounterId, "assigned-providers", "assignedProviders", "providers");

        return EncounterSummaryDto.builder()
                .meta(meta)
                .assignedProviders(mapAssignedProviders(mergedData))
                .chiefComplaints(mapChiefComplaints(mergedData))
                .vitals(mapVitals(patientId, encounterId))
                .hpi(mapHpi(mergedData))
                .pmh(mapPmh(mergedData))
                .patientMH(mapPatientMH(mergedData))
                .familyHistory(mapFamilyHistory(mergedData))
                .socialHistory(mapSocialHistory(mergedData))
                .ros(mapRos(mergedData))
                .physicalExam(mapPhysicalExam(mergedData))
                .procedures(mapProcedures(patientId, encounterId))
                .assessment(mapAssessment(mergedData))
                .plan(mapPlan(mergedData))
                .providerNotes(mapProviderNotes(patientId, encounterId))
                .providerSignature(mapProviderSignature(patientId, encounterId))
                .dateTimeFinalized(mapDateTimeFinalized(patientId, encounterId))
                .build();
    }

    /**
     * Load the encounter-form composition data for a given patient and encounter.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadEncounterFormData(Long patientId, Long encounterId) {
        try {
            Map<String, Object> result = fhirService.list("encounter-form", patientId, 0, 50);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content == null || content.isEmpty()) return Map.of();

            // Find the composition matching this encounter
            for (Map<String, Object> comp : content) {
                String encounterRef = str(comp, "encounterRef", "encounterReference");
                if (encounterRef != null && encounterRef.contains(String.valueOf(encounterId))) {
                    return comp;
                }
            }
            // If no encounter match found, return the first composition
            return content.get(0);
        } catch (Exception e) {
            log.error("Error loading encounter form data for patient {} encounter {}", patientId, encounterId, e);
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.ChiefComplaint> mapChiefComplaints(Map<String, Object> formData) {
        List<EncounterSummaryDto.ChiefComplaint> result = new java.util.ArrayList<>();
        // Try field keys: cc, chiefComplaint, chief_complaint
        for (String key : List.of("cc", "chiefComplaint", "chief_complaint", "cc_text", "chiefComplaintText")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.ChiefComplaint.builder().complaint(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.ChiefComplaint.builder()
                                .complaint(str((Map<String, Object>) m, "complaint", "text", "title", "value"))
                                .notes(str((Map<String, Object>) m, "notes", "description"))
                                .build());
                    } else if (item instanceof String s && !s.isBlank()) {
                        result.add(EncounterSummaryDto.ChiefComplaint.builder().complaint(s).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.HPIEntry> mapHpi(Map<String, Object> formData) {
        List<EncounterSummaryDto.HPIEntry> result = new java.util.ArrayList<>();
        for (String key : List.of("hpi", "hpi_text", "historyOfPresentIllness")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.HPIEntry.builder().description(s).text(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        String text = str((Map<String, Object>) m, "text", "description", "value");
                        result.add(EncounterSummaryDto.HPIEntry.builder().description(text).text(text).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.PMHEntry> mapPmh(Map<String, Object> formData) {
        List<EncounterSummaryDto.PMHEntry> result = new java.util.ArrayList<>();
        for (String key : List.of("pmh", "pmh_text", "pastMedicalHistory")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.PMHEntry.builder().description(s).text(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        String text = str((Map<String, Object>) m, "text", "description", "value");
                        result.add(EncounterSummaryDto.PMHEntry.builder().description(text).text(text).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.PatientMHEntry> mapPatientMH(Map<String, Object> formData) {
        List<EncounterSummaryDto.PatientMHEntry> result = new java.util.ArrayList<>();
        for (String key : List.of("patientMH", "patientMedicalHistory", "patient_medical_history", "medicalHistory")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.PatientMHEntry.builder().description(s).text(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        String text = str((Map<String, Object>) m, "text", "description", "value");
                        result.add(EncounterSummaryDto.PatientMHEntry.builder().description(text).text(text).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.FamilyHistory> mapFamilyHistory(Map<String, Object> formData) {
        List<EncounterSummaryDto.FamilyHistory> result = new java.util.ArrayList<>();
        for (String key : List.of("fh", "familyHistory", "family_history")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.FamilyHistory.builder().condition(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.FamilyHistory.builder()
                                .relation(str((Map<String, Object>) m, "relation", "relationship"))
                                .condition(str((Map<String, Object>) m, "condition", "text", "description", "value"))
                                .details(str((Map<String, Object>) m, "details", "notes"))
                                .build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.ROSEntry> mapRos(Map<String, Object> formData) {
        List<EncounterSummaryDto.ROSEntry> result = new java.util.ArrayList<>();
        for (String key : List.of("ros", "reviewOfSystems", "review_of_systems")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.ROSEntry.builder().system("General").finding(s).build());
            } else if (val instanceof Map<?,?> rosMap) {
                for (var entry : ((Map<String, Object>) rosMap).entrySet()) {
                    String system = entry.getKey();
                    Object v = entry.getValue();
                    String finding = v instanceof String ? (String) v : (v != null ? v.toString() : null);
                    if (finding != null && !finding.isBlank()) {
                        result.add(EncounterSummaryDto.ROSEntry.builder().system(system).finding(finding).build());
                    }
                }
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.ROSEntry.builder()
                                .system(str((Map<String, Object>) m, "system", "systemName"))
                                .finding(str((Map<String, Object>) m, "finding", "text", "value", "notes"))
                                .build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.PhysicalExam> mapPhysicalExam(Map<String, Object> formData) {
        List<EncounterSummaryDto.PhysicalExam> result = new java.util.ArrayList<>();
        for (String key : List.of("pe", "physicalExam", "physical_exam")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.PhysicalExam.builder().summary(s).build());
            } else if (val instanceof Map<?,?> peMap) {
                List<EncounterSummaryDto.PhysicalExamSection> sections = new java.util.ArrayList<>();
                for (var entry : ((Map<String, Object>) peMap).entrySet()) {
                    Object v = entry.getValue();
                    String findings = v instanceof String ? (String) v : (v != null ? v.toString() : null);
                    if (findings != null && !findings.isBlank()) {
                        sections.add(EncounterSummaryDto.PhysicalExamSection.builder()
                                .sectionKey(entry.getKey()).findings(findings).build());
                    }
                }
                if (!sections.isEmpty()) {
                    result.add(EncounterSummaryDto.PhysicalExam.builder().sections(sections).build());
                }
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.PhysicalExam.builder()
                                .summary(str((Map<String, Object>) m, "summary", "text", "findings"))
                                .build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.Assessment> mapAssessment(Map<String, Object> formData) {
        List<EncounterSummaryDto.Assessment> result = new java.util.ArrayList<>();
        for (String key : List.of("assessment", "assessment_text", "assessmentText")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.Assessment.builder().assessment(s).text(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        String text = str((Map<String, Object>) m, "assessment", "text", "description", "value");
                        result.add(EncounterSummaryDto.Assessment.builder().assessment(text).text(text).build());
                    } else if (item instanceof String s && !s.isBlank()) {
                        result.add(EncounterSummaryDto.Assessment.builder().assessment(s).text(s).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.Plan> mapPlan(Map<String, Object> formData) {
        List<EncounterSummaryDto.Plan> result = new java.util.ArrayList<>();
        for (String key : List.of("plan", "plan_text", "planText")) {
            Object val = formData.get(key);
            if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.Plan.builder().plan(s).build());
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.Plan.builder()
                                .plan(str((Map<String, Object>) m, "plan", "text", "value"))
                                .diagnosticPlan(str((Map<String, Object>) m, "diagnosticPlan"))
                                .notes(str((Map<String, Object>) m, "notes"))
                                .build());
                    } else if (item instanceof String s && !s.isBlank()) {
                        result.add(EncounterSummaryDto.Plan.builder().plan(s).build());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.AssignedProvider> mapAssignedProviders(Map<String, Object> formData) {
        List<EncounterSummaryDto.AssignedProvider> result = new java.util.ArrayList<>();
        for (String key : List.of("assignedProviders", "assigned_providers", "providers")) {
            Object val = formData.get(key);
            if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        result.add(EncounterSummaryDto.AssignedProvider.builder()
                                .name(str((Map<String, Object>) m, "name", "providerName", "text"))
                                .providerName(str((Map<String, Object>) m, "providerName", "name"))
                                .role(str((Map<String, Object>) m, "role"))
                                .build());
                    } else if (item instanceof String s && !s.isBlank()) {
                        result.add(EncounterSummaryDto.AssignedProvider.builder().name(s).providerName(s).build());
                    }
                }
            } else if (val instanceof String s && !s.isBlank()) {
                result.add(EncounterSummaryDto.AssignedProvider.builder().name(s).providerName(s).build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.Vitals> mapVitals(Long patientId, Long encounterId) {
        try {
            Map<String, Object> result = fhirService.list("vitals", patientId, 0, 50);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content == null || content.isEmpty()) return List.of();

            // Filter vitals by encounter reference
            List<EncounterSummaryDto.Vitals> vitals = new java.util.ArrayList<>();
            for (Map<String, Object> v : content) {
                String encRef = str(v, "encounterReference", "encounter", "encounterRef");
                if (encRef != null && encRef.contains(String.valueOf(encounterId))) {
                    vitals.add(EncounterSummaryDto.Vitals.builder()
                            .weightKg(asDouble(v.get("weightKg")))
                            .weightLbs(asDouble(v.get("weightLbs")))
                            .heightCm(asDouble(v.get("heightCm")))
                            .heightIn(asDouble(v.get("heightIn")))
                            .bpSystolic(asInt(v.get("bpSystolic")))
                            .bpDiastolic(asInt(v.get("bpDiastolic")))
                            .pulse(asInt(v.get("pulse")))
                            .respiration(asInt(v.get("respiration")))
                            .temperatureC(asDouble(v.get("temperatureC")))
                            .temperatureF(asDouble(v.get("temperatureF")))
                            .oxygenSaturation(asDouble(v.get("oxygenSaturation")))
                            .bmi(asDouble(v.get("bmi")))
                            .notes(str(v, "notes"))
                            .build());
                }
            }
            // If no encounter-specific vitals found, return all (for encounters without explicit ref)
            if (vitals.isEmpty() && !content.isEmpty()) {
                var v = content.get(0);
                vitals.add(EncounterSummaryDto.Vitals.builder()
                        .weightKg(asDouble(v.get("weightKg")))
                        .heightCm(asDouble(v.get("heightCm")))
                        .bpSystolic(asInt(v.get("bpSystolic")))
                        .bpDiastolic(asInt(v.get("bpDiastolic")))
                        .pulse(asInt(v.get("pulse")))
                        .temperatureC(asDouble(v.get("temperatureC")))
                        .oxygenSaturation(asDouble(v.get("oxygenSaturation")))
                        .bmi(asDouble(v.get("bmi")))
                        .build());
            }
            return vitals;
        } catch (Exception e) {
            log.error("Error mapping vitals for patient {} encounter {}", patientId, encounterId, e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<EncounterSummaryDto.Procedure> mapProcedures(Long patientId, Long encounterId) {
        try {
            Map<String, Object> result = fhirService.list("procedures", patientId, 0, 200);
            List<Map<String, Object>> allProcs = (List<Map<String, Object>>) result.get("content");
            if (allProcs == null) return List.of();

            // Filter by encounter
            String encRef = "Encounter/" + encounterId;
            List<Map<String, Object>> procedures = allProcs.stream()
                    .filter(p -> {
                        String ref = str(p, "encounterReference", "encounter");
                        return ref != null && ref.contains(String.valueOf(encounterId));
                    })
                    .collect(Collectors.toList());

            List<EncounterSummaryDto.Procedure> mapped = new java.util.ArrayList<>();
            for (Map<String, Object> d : procedures) {
                var builder = EncounterSummaryDto.Procedure.builder()
                        .id(asLong(d.get("id")))
                        .fhirId(str(d, "fhirId"))
                        .cpt4(str(d, "cpt4", "code"))
                        .description(str(d, "description"))
                        .procedureName(str(d, "note"))
                        .units(asInt(d.get("units")))
                        .rate(asDouble(d.get("rate")))
                        .relatedIcds(str(d, "relatedIcds"));

                Object codeItemsObj = d.get("codeItems");
                if (codeItemsObj instanceof List<?> codeItemsList && !codeItemsList.isEmpty()) {
                    List<EncounterSummaryDto.CodeItem> codeItems = new java.util.ArrayList<>();
                    for (Object ci : codeItemsList) {
                        if (ci instanceof Map<?, ?> raw) {
                            Map<String, Object> m = (Map<String, Object>) raw;
                            codeItems.add(EncounterSummaryDto.CodeItem.builder()
                                    .cpt4(str(m, "cpt4", "code"))
                                    .description(str(m, "description"))
                                    .units(asInt(m.get("units")))
                                    .rate(asDouble(m.get("rate")))
                                    .modifier1(str(m, "modifier1"))
                                    .note(str(m, "note"))
                                    .build());
                        }
                    }
                    builder.codeItems(codeItems);
                }

                mapped.add(builder.build());
            }
            return mapped;
        } catch (Exception e) {
            log.error("Error mapping procedures", e);
            return List.of();
        }
    }

    private List<EncounterSummaryDto.ProviderNote> mapProviderNotes(Long patientId, Long encounterId) {
        try {
            return providerNoteService.list(patientId, encounterId).stream()
                    .map(d -> EncounterSummaryDto.ProviderNote.builder()
                            .id(d.getId())
                            .fhirId(d.getFhirId())
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

    /**
     * Load data from an individual FHIR tab as fallback if the formData doesn't already have it.
     */
    @SuppressWarnings("unchecked")
    private void loadTabFallback(Map<String, Object> mergedData, Long patientId, Long encounterId, String tabKey, String... targetKeys) {
        // Check if any target key already has data
        for (String key : targetKeys) {
            Object existing = mergedData.get(key);
            if (existing != null) {
                if (existing instanceof List<?> list && !list.isEmpty()) return;
                if (existing instanceof String s && !s.isBlank()) return;
                if (existing instanceof Map<?,?> m && !m.isEmpty()) return;
            }
        }
        try {
            Map<String, Object> result = fhirService.list(tabKey, patientId, 0, 50);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content == null || content.isEmpty()) return;
            // Filter by encounter reference
            List<Map<String, Object>> matched = content.stream()
                    .filter(item -> {
                        String encRef = str(item, "encounterReference", "encounter", "encounterRef");
                        return encRef != null && encRef.contains(String.valueOf(encounterId));
                    })
                    .collect(Collectors.toList());
            if (matched.isEmpty()) matched = content; // fallback to all
            if (!matched.isEmpty() && targetKeys.length > 0) {
                mergedData.put(targetKeys[0], matched);
            }
        } catch (Exception e) {
            // Tab may not exist — silently ignore
            log.debug("Fallback tab load for '{}' failed: {}", tabKey, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private EncounterSummaryDto.SocialHistory mapSocialHistory(Map<String, Object> formData) {
        List<EncounterSummaryDto.SocialHistoryEntry> entries = new java.util.ArrayList<>();
        for (String key : List.of("socialHistory", "sh", "social_history", "socialHistory_entries")) {
            Object val = formData.get(key);
            if (val instanceof Map<?,?> shMap) {
                Object entryList = ((Map<String, Object>) shMap).get("entries");
                if (entryList instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?,?> m) {
                            entries.add(EncounterSummaryDto.SocialHistoryEntry.builder()
                                    .category(str((Map<String, Object>) m, "category", "type"))
                                    .value(str((Map<String, Object>) m, "value", "text", "status"))
                                    .details(str((Map<String, Object>) m, "details", "notes"))
                                    .build());
                        }
                    }
                }
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> m) {
                        entries.add(EncounterSummaryDto.SocialHistoryEntry.builder()
                                .category(str((Map<String, Object>) m, "category", "type"))
                                .value(str((Map<String, Object>) m, "value", "text", "status"))
                                .details(str((Map<String, Object>) m, "details", "notes"))
                                .build());
                    }
                }
            }
        }
        return entries.isEmpty() ? null : EncounterSummaryDto.SocialHistory.builder().entries(entries).build();
    }

    private String str(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String s && !s.isBlank()) return s;
            if (val != null) return val.toString();
        }
        return null;
    }

    private Long asLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Integer asInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Double asDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
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

        sb.append("<div class='header'>");
        sb.append("<div class='company'>Ciyex Health Solutions</div>");
        sb.append("<div class='info'>123 Main St, Chennai | +91 98765 43210 | info@ciyex.com</div>");
        sb.append("</div>");

        if (dto.getMeta() != null) {
            sb.append("<div class='section'><div class='title'>ENCOUNTER</div><div class='grid'>");
            if (dto.getMeta().getVisitCategory() != null) sb.append("<div><b>Visit:</b> ").append(escape(dto.getMeta().getVisitCategory())).append("</div>");
            if (dto.getMeta().getType() != null) sb.append("<div><b>Type:</b> ").append(escape(dto.getMeta().getType())).append("</div>");
            if (dto.getMeta().getFacility() != null) sb.append("<div><b>Facility:</b> ").append(escape(dto.getMeta().getFacility())).append("</div>");
            if (dto.getMeta().getDateOfService() != null) sb.append("<div><b>Date:</b> ").append(escape(dto.getMeta().getDateOfService())).append("</div>");
            if (dto.getMeta().getReasonForVisit() != null) sb.append("<div><b>Reason:</b> ").append(escape(dto.getMeta().getReasonForVisit())).append("</div>");
            sb.append("</div></div>");
        }

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

        if (dto.getChiefComplaints() != null && !dto.getChiefComplaints().isEmpty()) {
            sb.append("<div class='section'><div class='title'>CHIEF COMPLAINT</div>");
            for (var cc : dto.getChiefComplaints()) {
                sb.append("<b>").append(escape(cc.getComplaint())).append("</b>");
                if (cc.getNotes() != null) sb.append(": ").append(escape(cc.getNotes()));
                sb.append(" ");
            }
            sb.append("</div>");
        }

        if (dto.getVitals() != null && !dto.getVitals().isEmpty()) {
            sb.append("<div class='section'><div class='title'>VITALS</div><div class='grid'>");
            for (var v : dto.getVitals()) {
                if (v.getWeightKg() != null) sb.append("<div>Wt: ").append(v.getWeightKg()).append("kg</div>");
                if (v.getHeightCm() != null) sb.append("<div>Ht: ").append(v.getHeightCm()).append("cm</div>");
                if (v.getBpSystolic() != null && v.getBpDiastolic() != null)
                    sb.append("<div>BP: ").append(v.getBpSystolic()).append("/").append(v.getBpDiastolic()).append("</div>");
                if (v.getPulse() != null) sb.append("<div>Pulse: ").append(v.getPulse()).append("</div>");
                if (v.getTemperatureC() != null) sb.append("<div>Temp: ").append(v.getTemperatureC()).append("C</div>");
                if (v.getOxygenSaturation() != null) sb.append("<div>O2: ").append(v.getOxygenSaturation()).append("%</div>");
                if (v.getBmi() != null) sb.append("<div>BMI: ").append(v.getBmi()).append("</div>");
            }
            sb.append("</div></div>");
        }

        if (dto.getHpi() != null && !dto.getHpi().isEmpty()) {
            sb.append("<div class='section'><div class='title'>HISTORY OF PRESENT ILLNESS</div>");
            for (var h : dto.getHpi()) {
                if (h.getDescription() != null) sb.append(escape(h.getDescription())).append(" ");
                else if (h.getText() != null) sb.append(escape(h.getText())).append(" ");
            }
            sb.append("</div>");
        }

        if (dto.getRos() != null && !dto.getRos().isEmpty()) {
            sb.append("<div class='section'><div class='title'>REVIEW OF SYSTEMS</div><div class='grid'>");
            for (var r : dto.getRos()) {
                sb.append("<div class='item'>");
                if (r.getSystem() != null) sb.append("<b>").append(escape(r.getSystem())).append(":</b> ");
                if (r.getFinding() != null) sb.append(escape(r.getFinding()));
                else if (r.getNotes() != null) sb.append(escape(r.getNotes()));
                sb.append("</div>");
            }
            sb.append("</div></div>");
        }

        if (dto.getPmh() != null && !dto.getPmh().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PAST MEDICAL HISTORY</div>");
            for (var p : dto.getPmh()) {
                if (p.getDescription() != null) sb.append(escape(p.getDescription())).append("; ");
                else if (p.getText() != null) sb.append(escape(p.getText())).append("; ");
            }
            sb.append("</div>");
        }

        if (dto.getFamilyHistory() != null && !dto.getFamilyHistory().isEmpty()) {
            sb.append("<div class='section'><div class='title'>FAMILY HISTORY</div>");
            for (var fh : dto.getFamilyHistory()) {
                if (fh.getRelation() != null) sb.append("<b>").append(escape(fh.getRelation())).append(":</b> ");
                if (fh.getCondition() != null) sb.append(escape(fh.getCondition()));
                if (fh.getDetails() != null) sb.append(" (").append(escape(fh.getDetails())).append(")");
                sb.append("; ");
            }
            sb.append("</div>");
        }

        if (dto.getPhysicalExam() != null && !dto.getPhysicalExam().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PHYSICAL EXAMINATION</div>");
            for (var pe : dto.getPhysicalExam()) {
                if (pe.getSummary() != null) sb.append(escape(pe.getSummary())).append(" ");
                if (pe.getSections() != null) {
                    sb.append("<div class='grid'>");
                    for (var sec : pe.getSections()) {
                        sb.append("<div class='item'>");
                        if (sec.getSectionKey() != null) sb.append("<b>").append(escape(sec.getSectionKey())).append(":</b> ");
                        if (sec.getFindings() != null) sb.append(escape(sec.getFindings()));
                        sb.append("</div>");
                    }
                    sb.append("</div>");
                }
            }
            sb.append("</div>");
        }

        if (dto.getProcedures() != null && !dto.getProcedures().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PROCEDURES</div>");
            for (var proc : dto.getProcedures()) {
                if (proc.getCodeItems() == null || proc.getCodeItems().isEmpty()) {
                    if (proc.getCpt4() != null) sb.append("<b>").append(escape(proc.getCpt4())).append("</b> ");
                    if (proc.getDescription() != null) sb.append(escape(proc.getDescription())).append("; ");
                } else {
                    for (var item : proc.getCodeItems()) {
                        if (item.getCpt4() != null) sb.append("<b>").append(escape(item.getCpt4())).append("</b> ");
                        if (item.getDescription() != null) sb.append(escape(item.getDescription()));
                        if (item.getUnits() != null) sb.append(" (x").append(item.getUnits()).append(")");
                        if (item.getRate() != null) sb.append(" $").append(item.getRate());
                        sb.append("; ");
                    }
                }
            }
            sb.append("</div>");
        }

        if (dto.getAssessment() != null && !dto.getAssessment().isEmpty()) {
            sb.append("<div class='section'><div class='title'>ASSESSMENT</div>");
            int idx = 1;
            for (var a : dto.getAssessment()) {
                sb.append(idx++).append(". ").append(escape(a.getAssessment())).append(" ");
            }
            sb.append("</div>");
        }

        if (dto.getPlan() != null && !dto.getPlan().isEmpty()) {
            sb.append("<div class='section'><div class='title'>PLAN</div>");
            for (var p : dto.getPlan()) {
                if (p.getDiagnosticPlan() != null) sb.append("<b>Dx:</b> ").append(escape(p.getDiagnosticPlan())).append(" ");
                if (p.getPlan() != null) sb.append("<b>Plan:</b> ").append(escape(p.getPlan())).append(" ");
                if (p.getFollowUpVisit() != null) sb.append("<b>F/U:</b> ").append(escape(String.valueOf(p.getFollowUpVisit()))).append(" ");
            }
            sb.append("</div>");
        }

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

        if (dto.getProviderSignature() != null && dto.getProviderSignature().getSignedBy() != null) {
            sb.append("<div class='section'><div class='title'>SIGNATURE</div>");
            sb.append("<b>Signed:</b> ").append(escape(dto.getProviderSignature().getSignedBy()));
            if (dto.getProviderSignature().getSignedAt() != null)
                sb.append(" at ").append(escape(dto.getProviderSignature().getSignedAt()));
            sb.append("</div>");
        }

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
