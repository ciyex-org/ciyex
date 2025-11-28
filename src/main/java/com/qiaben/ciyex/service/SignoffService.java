




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SignoffDto;
import com.qiaben.ciyex.entity.Signoff;
import com.qiaben.ciyex.repository.SignoffRepository;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class SignoffService {
    public List<SignoffDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
                .stream().map(this::toDto).toList();
    }
    private final SignoffRepository repo;
    private final EncounterService encounterService;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired(required = false)
    private com.qiaben.ciyex.storage.fhir.FhirExternalSignoffStorage fhirStorage;

    @Autowired
    public SignoffService(SignoffRepository repo, EncounterService encounterService, 
                          ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repo = repo;
        this.encounterService = encounterService;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_DRAFT  = "Draft";
    private static final String STATUS_SIGNED = "Signed";
    private static final String STATUS_LOCKED = "Locked";

    // ---- CRUD


    public SignoffDto create(Long patientId, Long encounterId, SignoffDto dto) {
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        Signoff e = new Signoff();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        e.setStatus(STATUS_DRAFT);
        applyEditable(e, dto);
        e = repo.save(e);
        
        // Step 5: Optional external FHIR sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Signoff create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                log.info("Attempting FHIR sync for Signoff ID: {}", e.getId());
                ExternalStorage<SignoffDto> ext = storageResolver.resolve(SignoffDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                SignoffDto snapshot = toDto(e);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource for Signoff ID: {} with externalId: {}", e.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for Signoff ID: {}", e.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync Signoff to external storage", ex);
            }
        } else if (fhirStorage != null) {
            try {
                log.info("No storage type configured, falling back to direct FHIR storage for Signoff ID: {}", e.getId());
                SignoffDto snapshot = toDto(e);
                String externalId = fhirStorage.create(snapshot);
                log.info("FHIR fallback create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource (fallback) for Signoff ID: {} with externalId: {}", e.getId(), externalId);
                }
            } catch (Exception ex) {
                log.error("Failed to sync Signoff to external storage (fallback)", ex);
            }
        }
        
        if (e.getExternalId() == null) {
            String generatedId = "SO-" + System.currentTimeMillis();
            e.setExternalId(generatedId);
            e.setFhirId(generatedId);
            e = repo.save(e);
            log.info("Auto-generated externalId: {}", generatedId);
        } else {
            e.setFhirId(e.getExternalId());
            e = repo.save(e);
        }
        
        return toDto(e);
    }

    public List<SignoffDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    public SignoffDto getOne(Long patientId, Long encounterId, Long id) {
        Signoff e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sign-off not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        return toDto(e);
    }

    public SignoffDto update(Long patientId, Long encounterId, Long id, SignoffDto dto) {
        Signoff e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sign-off not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (isLocked(e)) throw new IllegalStateException("Signed/locked sign-offs are read-only.");
        applyEditable(e, dto);
        e = repo.save(e);

        // Step 7: Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Signoff update - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for Signoff ID: {}", e.getId());
                    ExternalStorage<SignoffDto> ext = storageResolver.resolve(SignoffDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    SignoffDto snapshot = toDto(e);
                    ext.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource for Signoff ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Signoff update to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Signoff ID: {}", e.getId());
                    SignoffDto snapshot = toDto(e);
                    fhirStorage.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource (fallback) for Signoff ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Signoff update to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Signoff ID: {}", e.getId());
            }
        }

        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        return toDto(e);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Signoff e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sign-off not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (isLocked(e)) throw new IllegalStateException("Signed/locked sign-offs cannot be deleted.");

        // Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Signoff delete - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR delete for Signoff ID: {}", e.getId());
                    ExternalStorage<SignoffDto> ext = storageResolver.resolve(SignoffDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ext.delete(e.getExternalId());
                    log.info("Deleted FHIR resource for Signoff ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Signoff delete to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Signoff ID: {}", e.getId());
                    fhirStorage.delete(e.getExternalId());
                    log.info("Deleted FHIR resource (fallback) for Signoff ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Signoff delete to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Signoff ID: {}", e.getId());
            }
        }

        repo.delete(e);
    }

    // ---- eSign

    public SignoffDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        Signoff e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sign-off not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));

        if (isLocked(e)) return toDto(e); // idempotent

        e.setStatus(STATUS_SIGNED);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        // schema is varchar — keep string
        e.setSignedAt(java.time.OffsetDateTime.now().toString());

        // If signatureData provided earlier, compute content hash for integrity
        if (StringUtils.hasText(e.getSignatureData())) {
            e.setContentHash(md5(e.getSignatureData()));
        }

        // Immediately lock after sign (optional but matches UI badge options)
        e.setStatus(STATUS_LOCKED);

        e = repo.save(e);
        return toDto(e);
    }

    // ---- Print PDF

    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        Signoff e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sign-off not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));

        // optional stamp
        try { e.setPrintedAt(LocalDateTime.now()); repo.save(e); } catch (Exception ignore) {}

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Encounter Sign-off");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Sign-off ID:", String.valueOf(id)); y -= 22;

                // Content
                if (StringUtils.hasText(e.getStatus()))         { draw(cs, x, y, "Status:", e.getStatus()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy()))       { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
                if (StringUtils.hasText(e.getSignerRole()))     { draw(cs, x, y, "Role:", e.getSignerRole()); y -= 16; }
                if (StringUtils.hasText(e.getSignedAt()))       { draw(cs, x, y, "Signed At:", e.getSignedAt()); y -= 16; }
                if (StringUtils.hasText(e.getSignatureType()))  { draw(cs, x, y, "Signature Type:", e.getSignatureType()); y -= 16; }
                if (StringUtils.hasText(e.getContentHash()))    { draw(cs, x, y, "Content Hash:", e.getContentHash()); y -= 16; }
                if (StringUtils.hasText(e.getTargetType()))     { draw(cs, x, y, "Target:", e.getTargetType() + (e.getTargetId() != null ? " #" + e.getTargetId() : "")); y -= 16; }

                if (StringUtils.hasText(e.getAttestationText())) {
                    y -= 10; draw(cs, x, y, "Attestation:", ""); y -= 14;
                    for (String ln : e.getAttestationText().split("\\R")) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 16, y); cs.showText(ln); cs.endText();
                        y -= 14;
                    }
                }
                if (StringUtils.hasText(e.getComments())) {
                    y -= 10; draw(cs, x, y, "Comments:", ""); y -= 14;
                    for (String ln : e.getComments().split("\\R")) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 16, y); cs.showText(ln); cs.endText();
                        y -= 14;
                    }
                }

                // Footer (audit)
                y -= 10;
                if (e.getCreatedAt() != null)    { draw(cs, x, y, "Created:", DAY.format(e.getCreatedAt().atZone(ZoneId.systemDefault()))); y -= 16; }
                if (e.getUpdatedAt() != null)    { draw(cs, x, y, "Updated:", DAY.format(e.getUpdatedAt().atZone(ZoneId.systemDefault()))); y -= 16; }
                if (e.getPrintedAt() != null)    { draw(cs, x, y, "Printed:", e.getPrintedAt().toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Sign-off PDF", ex);
        }
    }

    // ---- helpers

    private static boolean isLocked(Signoff e) {
        String s = e.getStatus();
        return "Signed".equalsIgnoreCase(s) || "Locked".equalsIgnoreCase(s) || "finalized".equalsIgnoreCase(s);
    }

    private static String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private void applyEditable(Signoff e, SignoffDto d) {
        e.setExternalId(d.getExternalId());
        e.setTargetType(d.getTargetType());
        e.setTargetId(d.getTargetId());
        e.setTargetVersion(d.getTargetVersion());
        e.setSignerRole(d.getSignerRole());
        e.setSignatureType(d.getSignatureType());
        e.setSignatureData(d.getSignatureData());
        e.setAttestationText(d.getAttestationText());
        e.setComments(d.getComments());
        // signedBy/signedAt/contentHash/status managed via eSign
    }

    private SignoffDto toDto(Signoff e) {
        SignoffDto d = new SignoffDto();
        d.setId(e.getId());
        d.setExternalId(e.getFhirId());
        d.setFhirId(e.getFhirId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setTargetType(e.getTargetType());
        d.setTargetId(e.getTargetId());
        d.setTargetVersion(e.getTargetVersion());
        d.setStatus(e.getStatus());
        d.setSignedBy(e.getSignedBy());
        d.setSignerRole(e.getSignerRole());
        d.setSignedAt(e.getSignedAt());
        d.setSignatureType(e.getSignatureType());
        d.setSignatureData(e.getSignatureData());
        d.setContentHash(e.getContentHash());
        d.setAttestationText(e.getAttestationText());
        d.setComments(e.getComments());
        d.setPrintedAt(e.getPrintedAt() != null ? e.getPrintedAt().toString() : null);

        SignoffDto.Audit a = new SignoffDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DAY.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DAY.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }
}
