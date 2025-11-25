////package com.qiaben.ciyex.service;
////
////import com.qiaben.ciyex.dto.ChiefComplaintDto;
////import com.qiaben.ciyex.entity.ChiefComplaint;
////import com.qiaben.ciyex.repository.ChiefComplaintRepository;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.util.List;
////import java.util.stream.Collectors;
////
////@Service
////public class ChiefComplaintService {
////
////    private final ChiefComplaintRepository chiefComplaintRepository;
////
////    public ChiefComplaintService(ChiefComplaintRepository chiefComplaintRepository) {
////        this.chiefComplaintRepository = chiefComplaintRepository;
////    }
////
////    // Create Chief Complaint for a specific encounter
////    @Transactional
////    public ChiefComplaintDto create(ChiefComplaintDto dto) {
////        ChiefComplaint chiefComplaint = new ChiefComplaint();
////        chiefComplaint.setComplaint(dto.getComplaint());
////        chiefComplaint.setDetails(dto.getDetails());
////        chiefComplaint.setEncounterId(dto.getEncounterId());

////        chiefComplaint.setPatientId(dto.getPatientId());
////        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);
////        return mapToDto(chiefComplaint);
////    }
////
////    // Get all Chief Complaints for a specific encounter
////    @Transactional(readOnly = true)
////    public List<ChiefComplaintDto> getByEncounterId(Long encounterId) {
////        List<ChiefComplaint> complaints = chiefComplaintRepository.findByEncounterId(encounterId);
////        return complaints.stream().map(this::mapToDto).collect(Collectors.toList());
////    }
////
////    // Update Chief Complaint
////    @Transactional
////    public ChiefComplaintDto update(Long encounterId, Long id, ChiefComplaintDto dto) {
////        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
////                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
////
////        // Ensure the encounterId matches
////        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
////            throw new RuntimeException("Encounter ID mismatch");
////        }
////
////        // Set the updated data
////        chiefComplaint.setComplaint(dto.getComplaint());
////        chiefComplaint.setDetails(dto.getDetails());

////        chiefComplaint.setPatientId(dto.getPatientId());
////
////        // Save the updated chief complaint
////        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);
////
////        // Return the updated DTO
////        return mapToDto(chiefComplaint);
////    }
////
////
////    // Delete Chief Complaint
////    @Transactional
////    public void delete(Long encounterId, Long id) {
////        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
////                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
////        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
////            throw new RuntimeException("Encounter ID mismatch");
////        }
////        chiefComplaintRepository.delete(chiefComplaint);
////    }
////
////    private ChiefComplaintDto mapToDto(ChiefComplaint chiefComplaint) {
////        ChiefComplaintDto dto = new ChiefComplaintDto();
////        dto.setId(chiefComplaint.getId());
////        dto.setComplaint(chiefComplaint.getComplaint());
////        dto.setDetails(chiefComplaint.getDetails());
////        dto.setEncounterId(chiefComplaint.getEncounterId());
////        dto.setOrgId(chiefComplaint.getOrgId());
////        dto.setCreatedAt(chiefComplaint.getCreatedAt());
////        dto.setUpdatedAt(chiefComplaint.getUpdatedAt());
////        dto.setPatientId(chiefComplaint.getPatientId());
////        return dto;
////    }
////}
//
//
//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ChiefComplaintDto;
//import com.qiaben.ciyex.entity.ChiefComplaint;
//import com.qiaben.ciyex.repository.ChiefComplaintRepository;
//import com.qiaben.ciyex.storage.ExternalChiefComplaintStorage;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ChiefComplaintService {
//
//    private final ChiefComplaintRepository chiefComplaintRepository;
//    private final ExternalChiefComplaintStorage externalChiefComplaintStorage;
//
//    public ChiefComplaintService(ChiefComplaintRepository chiefComplaintRepository, ExternalChiefComplaintStorage externalChiefComplaintStorage) {
//        this.chiefComplaintRepository = chiefComplaintRepository;
//        this.externalChiefComplaintStorage = externalChiefComplaintStorage;
//    }
//
//    // Create a new Chief Complaint
//    public ChiefComplaintDto create(ChiefComplaintDto dto) {
//        // Convert DTO to entity
//        ChiefComplaint chiefComplaint = new ChiefComplaint();
//        chiefComplaint.setComplaint(dto.getComplaint());
//        chiefComplaint.setDetails(dto.getDetails());
//        chiefComplaint.setEncounterId(dto.getEncounterId());
//        chiefComplaint.setOrgId(RequestContext.get().getTenantName());
//        chiefComplaint.setPatientId(dto.getPatientId());
//        chiefComplaint.setCreatedAt(dto.getCreatedAt());
//        chiefComplaint.setUpdatedAt(dto.getUpdatedAt());
//
//        // Save to database
//        ChiefComplaint savedComplaint = chiefComplaintRepository.save(chiefComplaint);
//
//        // Save to external storage (e.g., FHIR)
//        externalChiefComplaintStorage.saveChiefComplaint(dto);
//
//        // Return the DTO with the saved data
//        dto.setId(savedComplaint.getId());
//        return dto;
//    }
//
//    // Get all Chief Complaints for a patient by encounter
//    public List<ChiefComplaintDto> getByPatientIdAndEncounterId(Long patientId, Long encounterId) {
//        List<ChiefComplaint> complaints = chiefComplaintRepository.findByPatientId(patientId);
//        return complaints.stream()
//                .filter(complaint -> complaint.getEncounterId().equals(encounterId))
//                .map(this::mapToDto)
//                .collect(Collectors.toList());
//    }
//
//    // Get a specific Chief Complaint by ID, patient ID, and encounter ID
//    public ChiefComplaintDto getById(Long patientId, Long encounterId, Long id) {
//        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
//                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
//
//        if (!complaint.getEncounterId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        return mapToDto(complaint);
//    }
//
//    // Update a Chief Complaint
//    public ChiefComplaintDto update(Long patientId, Long encounterId, Long id, ChiefComplaintDto dto) {
//        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
//                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
//
//        if (!complaint.getEncounterId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        // Update fields
//        complaint.setComplaint(dto.getComplaint());
//        complaint.setDetails(dto.getDetails());
//        complaint.setUpdatedAt(dto.getUpdatedAt());
//
//        // Save to database
//        ChiefComplaint updatedComplaint = chiefComplaintRepository.save(complaint);
//
//        // Save to external storage (e.g., FHIR)
//        externalChiefComplaintStorage.saveChiefComplaint(dto);
//
//        return mapToDto(updatedComplaint);
//    }
//
//    // Delete a Chief Complaint
//    public void delete(Long patientId, Long encounterId, Long id) {
//        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
//                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
//
//        if (!complaint.getEncounterId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        chiefComplaintRepository.delete(complaint);
//
//        // Optionally delete from external storage (e.g., FHIR)
//        externalChiefComplaintStorage.saveChiefComplaint(null);
//    }
//
//    // Helper method to map entity to DTO
//    private ChiefComplaintDto mapToDto(ChiefComplaint complaint) {
//        ChiefComplaintDto dto = new ChiefComplaintDto();
//        dto.setId(complaint.getId());
//        dto.setComplaint(complaint.getComplaint());
//        dto.setDetails(complaint.getDetails());
//        dto.setEncounterId(complaint.getEncounterId());
//        dto.setOrgId(complaint.getOrgId());
//        dto.setPatientId(complaint.getPatientId());
//        dto.setCreatedAt(complaint.getCreatedAt());
//        dto.setUpdatedAt(complaint.getUpdatedAt());
//        return dto;
//    }
//}
//
//
//




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ChiefComplaintDto;
import com.qiaben.ciyex.entity.ChiefComplaint;
import com.qiaben.ciyex.repository.ChiefComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChiefComplaintService {
    public List<ChiefComplaintDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }

    private final ChiefComplaintRepository repo;
    private final EncounterService encounterService;
    private final com.qiaben.ciyex.repository.PatientRepository patientRepository;
    private final com.qiaben.ciyex.repository.EncounterRepository encounterRepository;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // CREATE
    public ChiefComplaintDto create(Long patientId, Long encounterId, ChiefComplaintDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Validate Encounter exists and belongs to the Patient
        var encounterOpt = encounterRepository.findByIdAndPatientId(encounterId, patientId);
        if (encounterOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct and that the encounter belongs to this patient.",
                    encounterId, patientId)
            );
        }

        // Step 3: Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Step 4: Create the chief complaint
        ChiefComplaint e = new ChiefComplaint();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyEditable(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // LIST
    public List<ChiefComplaintDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // GET ONE
    public ChiefComplaintDto getOne(Long patientId, Long encounterId, Long id) {
        ChiefComplaint e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chief Complaint not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));
        return toDto(e);
    }

    // UPDATE (blocked if signed)
    public ChiefComplaintDto update(Long patientId, Long encounterId, Long id, ChiefComplaintDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Validate Encounter exists and belongs to the Patient
        var encounterOpt = encounterRepository.findByIdAndPatientId(encounterId, patientId);
        if (encounterOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct and that the encounter belongs to this patient.",
                    encounterId, patientId)
            );
        }

        // Step 3: Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Step 4: Find the chief complaint
        ChiefComplaint e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chief Complaint not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        // Step 5: Check if chief complaint itself is signed
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed chief complaint is read-only.");
        }

        // Step 6: Update the chief complaint
        applyEditable(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // DELETE (blocked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        ChiefComplaint e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chief Complaint not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed chief complaint cannot be deleted.");
        }
        repo.delete(e);
    }

    // ESIGN (no request body; idempotent)
    public ChiefComplaintDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        ChiefComplaint e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chief Complaint not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // PRINT (PDF) — stamps printedAt
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        ChiefComplaint e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chief Complaint not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        e.setPrintedAt(OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Chief Complaint");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", String.valueOf(id)); y -= 20;

                if (StringUtils.hasText(e.getComplaint())) { draw(cs, x, y, "Complaint:", e.getComplaint()); y -= 16; }
                if (StringUtils.hasText(e.getSeverity()))  { draw(cs, x, y, "Severity:", e.getSeverity());   y -= 16; }
                if (StringUtils.hasText(e.getStatus()))    { draw(cs, x, y, "Status:", e.getStatus());       y -= 16; }

                if (StringUtils.hasText(e.getDetails())) {
                    y -= 8;
                    draw(cs, x, y, "Details:", ""); y -= 14;
                    for (String ln : e.getDetails().split("\\R")) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12);
                        cs.newLineAtOffset(x + 16, y); cs.showText(ln); cs.endText(); y -= 14;
                    }
                }

                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().format(ISO)); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }

                y -= 10;
                if (e.getCreatedAt() != null) { draw(cs, x, y, "Created:", e.getCreatedAt().toString()); y -= 16; }
                if (e.getUpdatedAt() != null) { draw(cs, x, y, "Updated:", e.getUpdatedAt().toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Chief Complaint PDF", ex);
        }
    }

    // helpers
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private ChiefComplaintDto toDto(ChiefComplaint e) {
        ChiefComplaintDto d = new ChiefComplaintDto();
        d.setId(e.getId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setComplaint(e.getComplaint());
        d.setDetails(e.getDetails());
        d.setSeverity(e.getSeverity());
        d.setStatus(e.getStatus());
        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt() != null ? e.getSignedAt().format(ISO) : null);
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt() != null ? e.getPrintedAt().format(ISO) : null);
        d.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        d.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        return d;
    }

    private void applyEditable(ChiefComplaint e, ChiefComplaintDto d) {
        e.setComplaint(d.getComplaint());
        e.setDetails(d.getDetails());
        e.setSeverity(d.getSeverity());
        e.setStatus(d.getStatus());
        // eSign fields are set only by eSign()
    }
}
