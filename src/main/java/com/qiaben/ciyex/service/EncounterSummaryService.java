// src/main/java/com/qiaben/ciyex/service/EncounterSummaryService.java
package com.qiaben.ciyex.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.EncounterSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncounterSummaryService {

    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate rt = new RestTemplate();

    // Base URL of THIS app (so we can call our own endpoints).
    // Change to your host/port if needed.
    @Value("${app.selfBaseUrl:http://localhost:8080}")
    private String selfBaseUrl;

    /**
     * Main loader used by the controller JSON + PDF.
     * Pulls each section from the existing endpoints your UI already uses.
     */
    public EncounterSummaryDto load(Long patientId, Long encounterId) {
        String p = String.valueOf(patientId);
        String e = String.valueOf(encounterId);

        // --- meta: you may already have a meta endpoint; if not we keep a light stub here
        EncounterSummaryDto.EncounterMeta meta = EncounterSummaryDto.EncounterMeta.builder()
                .visitCategory("Outpatient")
                .type("Routine Checkup")
                .facility("Main Clinic")
                .dateOfService("2025-09-19")
                .reasonForVisit("Follow-up")
                .build();

        // Helper to try multiple URLs and return the first non-null "data"
        Supplier<HttpHeaders> jsonHeaders = () -> {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
            return h;
        };

        // Assigned Providers
        List<EncounterSummaryDto.AssignedProvider> assignedProviders =
                firstNonNullDataList(
                        List.of(
                                url("/api/assigned-providers/%s/%s", p, e),
                                url("/api/assigned/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.AssignedProvider>>() {}
                );

        // Chief Complaint
        List<EncounterSummaryDto.ChiefComplaint> chiefComplaints =
                firstNonNullDataList(
                        List.of(
                                url("/api/chief-complaint/%s/%s", p, e),
                                url("/api/chief-complaints/%s/%s", p, e),
                                url("/api/cc/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.ChiefComplaint>>() {}
                );

        // HPI
        List<EncounterSummaryDto.HPIEntry> hpi =
                firstNonNullDataList(
                        List.of(
                                url("/api/history-of-present-illness/%s/%s", p, e),
                                url("/api/hpi/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.HPIEntry>>() {}
                );

        // PMH
        List<EncounterSummaryDto.PMHEntry> pmh =
                firstNonNullDataList(
                        List.of(
                                url("/api/pmh/%s/%s", p, e),
                                url("/api/past-medical-history/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.PMHEntry>>() {}
                );

        // Patient Medical History
        List<EncounterSummaryDto.PatientMHEntry> patientMH =
                firstNonNullDataList(
                        List.of(
                                url("/api/patient-medical-history/%s/%s", p, e),
                                url("/api/patient-mh/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.PatientMHEntry>>() {}
                );

        // Family History
        List<EncounterSummaryDto.FamilyHistory> familyHistory =
                firstNonNullDataList(
                        List.of(
                                url("/api/family-history/%s/%s", p, e),
                                url("/api/fh/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.FamilyHistory>>() {}
                );

        // Social History (array or object.entries)
        EncounterSummaryDto.SocialHistory socialHistory =
                firstNonNullDataObject(
                        List.of(
                                url("/api/social-history/%s/%s", p, e),
                                url("/api/socialhistory/%s/%s", p, e),
                                url("/api/sh/%s/%s", p, e)
                        ),
                        new TypeReference<EncounterSummaryDto.SocialHistory>() {}
                );

        // ROS
        List<EncounterSummaryDto.ROSEntry> ros =
                firstNonNullDataList(
                        List.of(
                                url("/api/reviewofsystems/%s/%s", p, e),
                                url("/api/ros/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.ROSEntry>>() {}
                );

        // Physical Exam
        List<EncounterSummaryDto.PhysicalExam> physicalExam =
                firstNonNullDataList(
                        List.of(
                                url("/api/physical-exam/%s/%s", p, e),
                                url("/api/pe/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.PhysicalExam>>() {}
                );

        // Procedures
        List<EncounterSummaryDto.Procedure> procedures =
                firstNonNullDataList(
                        List.of(
                                url("/api/procedures/%s/%s", p, e),
                                url("/api/procedure/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.Procedure>>() {}
                );

                

        // Codes
        List<EncounterSummaryDto.Code> codes =
                firstNonNullDataList(
                        List.of(url("/api/codes/%s/%s", p, e)),
                        new TypeReference<List<EncounterSummaryDto.Code>>() {}
                );

        // Assessment
        List<EncounterSummaryDto.Assessment> assessment =
                firstNonNullDataList(
                        List.of(
                                url("/api/assessment/%s/%s", p, e),
                                url("/api/assessments/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.Assessment>>() {}
                );

        // Plan
        List<EncounterSummaryDto.Plan> plan =
                firstNonNullDataList(
                        List.of(
                                url("/api/plan/%s/%s", p, e),
                                url("/api/plans/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.Plan>>() {}
                );

        // Provider Notes (SOAP)
        List<EncounterSummaryDto.ProviderNote> providerNotes =
                firstNonNullDataList(
                        List.of(
                                url("/api/provider-notes/%s/%s", p, e),
                                url("/api/soap/%s/%s", p, e)
                        ),
                        new TypeReference<List<EncounterSummaryDto.ProviderNote>>() {}
                );

        // Provider Signature
        EncounterSummaryDto.ProviderSignature providerSignature =
                firstNonNullDataObject(
                        List.of(
                                url("/api/provider-signatures/%s/%s", p, e),
                                url("/api/signatures/%s/%s", p, e)
                        ),
                        new TypeReference<EncounterSummaryDto.ProviderSignature>() {}
                );

        // Signoff
        EncounterSummaryDto.Signoff signoff =
                firstNonNullDataObject(
                        List.of(
                                url("/api/signoffs/%s/%s", p, e),
                                url("/api/sign-off/%s/%s", p, e)
                        ),
                        new TypeReference<EncounterSummaryDto.Signoff>() {}
                );

        // Date / Time Finalized
        EncounterSummaryDto.DateTimeFinalized dateTimeFinalized =
                firstNonNullDataObject(
                        List.of(
                                url("/api/datetime-finalized/%s/%s", p, e),
                                url("/api/finalized/%s/%s", p, e)
                        ),
                        new TypeReference<EncounterSummaryDto.DateTimeFinalized>() {}
                );

        return EncounterSummaryDto.builder()
                .meta(meta)
                .assignedProviders(assignedProviders)
                .chiefComplaints(chiefComplaints)
                .hpi(hpi)
                .pmh(pmh)
                .patientMH(patientMH)
                .familyHistory(familyHistory)
                .socialHistory(socialHistory)
                .ros(ros)
                .physicalExam(physicalExam)
                .procedures(procedures)
                .codes(codes)
                .assessment(assessment)
                .plan(plan)
                .providerNotes(providerNotes)
                .providerSignature(providerSignature)
                .signoff(signoff)
                .dateTimeFinalized(dateTimeFinalized)
                .build();
    }

    // ---------- HTML → PDF helpers (unchanged from before) ----------
    public String buildHtml(EncounterSummaryDto dto) {
                StringBuilder sb = new StringBuilder();
                sb.append("<!doctype html><html><head><meta charset='utf-8'/>");
                sb.append("<style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;padding:24px;}h1{font-size:20px;margin:0 0 8px} .card{border:1px solid #e5e7eb;border-radius:12px;padding:12px;margin:10px 0} .header{display:flex;align-items:flex-start;margin-bottom:24px;} .logo{margin-right:24px;} .company-info{font-size:14px;line-height:1.5;} .company-title{font-weight:700;font-size:18px;} .section-title{font-weight:600;font-size:20px;margin:24px 0 8px 0;} .divider{border-bottom:2px solid #eee;margin-bottom:16px;}</style>");
                sb.append("</head><body>");
                // Company logo and contact details
                sb.append("<div class='header'>");
                sb.append("<div class='logo'><img src='https://ciyex.com/logo.png' alt='Ciyex Logo' width='80' height='80'/></div>");
                sb.append("<div class='company-info'>");
                sb.append("<div class='company-title'>Ciyex Health Solutions</div>");
                sb.append("<div>123 Main Street, Chennai, India</div>");
                sb.append("<div>Phone: +91 98765 43210</div>");
                sb.append("<div>Email: info@ciyex.com</div>");
                sb.append("</div></div>");
                sb.append("<div class='divider'></div>");
                sb.append("<div class='section-title'>Encounter Details</div>");
                if (dto.getMeta() != null) {
                        sb.append("<div class='card'>");
                        writeRow(sb, "Visit Category", dto.getMeta().getVisitCategory());
                        writeRow(sb, "Type", dto.getMeta().getType());
                        writeRow(sb, "Facility", dto.getMeta().getFacility());
                        writeRow(sb, "Date of Service", dto.getMeta().getDateOfService());
                        writeRow(sb, "Reason for Visit", dto.getMeta().getReasonForVisit());
                        sb.append("</div>");
                }
                // TODO: Add more sections for full details as needed
                sb.append("<div class='divider'></div>");
                sb.append("<div class='section-title'>Summary</div>");
                // You can add summary fields here
                sb.append("</body></html>");
                return sb.toString();
    }

    public byte[] renderPdfFromHtml(String html) {
        // Keep your existing OpenHTMLtoPDF logic here
        throw new UnsupportedOperationException("Implement with OpenHTMLtoPDF as in previous message.");
    }

    private static void writeRow(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return;
        sb.append("<div><b>").append(escape(label)).append(":</b> ").append(escape(value)).append("</div>");
    }
    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#039;");
    }

    // ---------- small HTTP helpers ----------

    private String url(String pattern, String... args) {
        return selfBaseUrl + String.format(pattern, (Object[]) args);
    }

    private <T> T firstNonNullDataObject(List<String> urls, TypeReference<T> type) {
        for (String u : urls) {
            T v = fetchData(u, type);
            if (v != null) return v;
        }
        return null;
    }

    private <T> List<T> firstNonNullDataList(List<String> urls, TypeReference<List<T>> type) {
        for (String u : urls) {
            List<T> v = fetchData(u, type);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    /**
     * Calls an endpoint that returns { success, message, data } and extracts `data`.
     * Accepts 200 only; swallows 404/401 etc. and returns null.
     */
    private <T> T fetchData(String url, TypeReference<T> type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, entity, String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return null;

            Map<String, Object> envelope = om.readValue(res.getBody(), new TypeReference<Map<String, Object>>() {});
            Object data = envelope.get("data");
            if (data == null) return null;

            // Convert `data` object into the requested type
            String json = om.writeValueAsString(data);
            return om.readValue(json, type);
        } catch (Exception ex) {
            log.debug("fetchData failed for {}: {}", url, ex.getMessage());
            return null;
        }
    }
}
