//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.SignoffDto;
//import com.qiaben.ciyex.entity.Signoff;
//import com.qiaben.ciyex.repository.SignoffRepository;
//import com.qiaben.ciyex.storage.ExternalSignoffStorage;
//import org.springframework.transaction.annotation.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class SignoffService {
//
//    private final SignoffRepository repo;
//    private final Optional<ExternalSignoffStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public SignoffDto create(Long orgId, Long patientId, Long encounterId, SignoffDto in) {
//        Signoff e = Signoff.builder()
//                .orgId(orgId).patientId(patientId).encounterId(encounterId)
//                .targetType(in.getTargetType())
//                .targetId(in.getTargetId())
//                .targetVersion(in.getTargetVersion())
//                .status(in.getStatus())
//                .signedBy(in.getSignedBy())
//                .signerRole(in.getSignerRole())
//                .signedAt(in.getSignedAt())
//                .signatureType(in.getSignatureType())
//                .signatureData(in.getSignatureData())
//                .contentHash(in.getContentHash())
//                .attestationText(in.getAttestationText())
//                .comments(in.getComments())
//                .build();
//
//        final Signoff saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final Signoff ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//        return mapToDto(saved);
//    }
//
//    public SignoffDto update(Long orgId, Long patientId, Long encounterId, Long id, SignoffDto in) {
//        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));
//
//        e.setTargetType(in.getTargetType());
//        e.setTargetId(in.getTargetId());
//        e.setTargetVersion(in.getTargetVersion());
//        e.setStatus(in.getStatus());
//        e.setSignedBy(in.getSignedBy());
//        e.setSignerRole(in.getSignerRole());
//        e.setSignedAt(in.getSignedAt());
//        e.setSignatureType(in.getSignatureType());
//        e.setSignatureData(in.getSignatureData());
//        e.setContentHash(in.getContentHash());
//        e.setAttestationText(in.getAttestationText());
//        e.setComments(in.getComments());
//
//        final Signoff updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final Signoff ref = updated;
//            if (ref.getExternalId() != null) ext.update(ref.getExternalId(), mapToDto(ref));
//        });
//        return mapToDto(updated);
//    }
//
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));
//        external.ifPresent(ext -> {
//            if (e.getExternalId() != null) ext.delete(e.getExternalId());
//        });
//        repo.delete(e);
//    }
//
//    public SignoffDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));
//        return mapToDto(e);
//    }
//
//    public List<SignoffDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<SignoffDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    private SignoffDto mapToDto(Signoff e) {
//        SignoffDto dto = new SignoffDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setTargetType(e.getTargetType());
//        dto.setTargetId(e.getTargetId());
//        dto.setTargetVersion(e.getTargetVersion());
//        dto.setStatus(e.getStatus());
//        dto.setSignedBy(e.getSignedBy());
//        dto.setSignerRole(e.getSignerRole());
//        dto.setSignedAt(e.getSignedAt());
//        dto.setSignatureType(e.getSignatureType());
//        dto.setSignatureData(e.getSignatureData());
//        dto.setContentHash(e.getContentHash());
//        dto.setAttestationText(e.getAttestationText());
//        dto.setComments(e.getComments());
//
//        SignoffDto.Audit a = new SignoffDto.Audit();
//        if (e.getCreatedAt()!=null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt()!=null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        dto.setAudit(a);
//        return dto;
//    }
//
//}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SignoffDto;
import com.qiaben.ciyex.entity.Signoff;
import com.qiaben.ciyex.repository.SignoffRepository;
import com.qiaben.ciyex.storage.ExternalSignoffStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignoffService {

    private final SignoffRepository repo;
    private final Optional<ExternalSignoffStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public SignoffDto create(Long orgId, Long patientId, Long encounterId, SignoffDto in) {
        Signoff e = Signoff.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .targetType(in.getTargetType())
                .targetId(in.getTargetId())
                .targetVersion(in.getTargetVersion())
                .status(in.getStatus())
                .signedBy(in.getSignedBy())
                .signerRole(in.getSignerRole())
                .signedAt(in.getSignedAt())
                .signatureType(in.getSignatureType())
                .signatureData(in.getSignatureData())
                .contentHash(in.getContentHash())
                .attestationText(in.getAttestationText())
                .comments(in.getComments())
                .build();

        final Signoff saved = repo.save(e);

        external.ifPresent(ext -> {
            String extId = ext.create(mapToDto(saved));
            saved.setExternalId(extId);
            repo.save(saved);
        });

        return mapToDto(saved);
    }

    @Transactional
    public SignoffDto update(Long orgId, Long patientId, Long encounterId, Long id, SignoffDto in) {
        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));

        e.setTargetType(in.getTargetType());
        e.setTargetId(in.getTargetId());
        e.setTargetVersion(in.getTargetVersion());
        e.setStatus(in.getStatus());
        e.setSignedBy(in.getSignedBy());
        e.setSignerRole(in.getSignerRole());
        e.setSignedAt(in.getSignedAt());
        e.setSignatureType(in.getSignatureType());
        e.setSignatureData(in.getSignatureData());
        e.setContentHash(in.getContentHash());
        e.setAttestationText(in.getAttestationText());
        e.setComments(in.getComments());

        final Signoff updated = repo.save(e);

        external.ifPresent(ext -> {
            if (updated.getExternalId() != null) {
                ext.update(updated.getExternalId(), mapToDto(updated));
            }
        });

        return mapToDto(updated);
    }

    @Transactional
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) {
                ext.delete(e.getExternalId());
            }
        });

        repo.delete(e);
    }

    @Transactional(readOnly = true)
    public SignoffDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        Signoff e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Signoff not found"));
        return mapToDto(e);
    }

    @Transactional(readOnly = true)
    public List<SignoffDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SignoffDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    private SignoffDto mapToDto(Signoff e) {
        SignoffDto dto = new SignoffDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setTargetType(e.getTargetType());
        dto.setTargetId(e.getTargetId());
        dto.setTargetVersion(e.getTargetVersion());
        dto.setStatus(e.getStatus());
        dto.setSignedBy(e.getSignedBy());
        dto.setSignerRole(e.getSignerRole());
        dto.setSignedAt(e.getSignedAt());
        dto.setSignatureType(e.getSignatureType());
        dto.setSignatureData(e.getSignatureData());
        dto.setContentHash(e.getContentHash());
        dto.setAttestationText(e.getAttestationText());
        dto.setComments(e.getComments());

        SignoffDto.Audit a = new SignoffDto.Audit();
        if (e.getCreatedAt() != null) {
            a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        }
        if (e.getUpdatedAt() != null) {
            a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        }
        dto.setAudit(a);
        return dto;
    }
}
