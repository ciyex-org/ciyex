package com.qiaben.ciyex.service;



import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.entity.EncounterStatus;
import com.qiaben.ciyex.repository.EncounterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ Spring Transactional

import java.util.Optional;

@Service
public class EncounterBrowserService {

    private final EncounterRepository encounterRepository; // ✅ inject repo

    public EncounterBrowserService(EncounterRepository encounterRepository) {
        this.encounterRepository = encounterRepository;
    }

    @Transactional(readOnly = true) // ✅ now valid
    public Page<EncounterDto> listAll(Optional<EncounterStatus> statusOpt,
            boolean recentOnly,
            int recentCount,
            Pageable pageable
    ) {
        // Build an effective pageable (don’t mutate the captured one)
        Pageable effective = pageable;
        if (recentOnly) {
            effective = PageRequest.of(
                    0,
                    Math.max(1, recentCount),
                    Sort.by(Sort.Direction.DESC, "encounterDate")
            );
        } else if (effective.getSort().isUnsorted()) {
            effective = PageRequest.of(
                    effective.getPageNumber(),
                    effective.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "encounterDate")
            );
        }

        // ✅ capture a final copy for lambda use
        final Pageable p = effective;

        Page<Encounter> page = statusOpt
                .map(st -> encounterRepository.findByStatus(st, p))
                .orElseGet(() -> encounterRepository.findAll(p));

        return page.map(this::mapToDto);
    }

    // Simple mapper (adapt to your fields)
    // In EncounterBrowseService or EncounterService (where your listAllForOrg() lives)
    private EncounterDto mapToDto(Encounter e) {
        EncounterDto dto = new EncounterDto();

        // IDs & scope
        dto.setId(e.getId());
        dto.setPatientId(e.getPatientId());

        // Core encounter info
        dto.setVisitCategory(e.getVisitCategory());               // e.g., "OPD", "ER"
        dto.setEncounterProvider(e.getEncounterProvider());       // e.g., "Dr. Smith"
        dto.setType(e.getType());                                 // e.g., "Consultation"
        dto.setSensitivity(e.getSensitivity());                   // e.g., "High"/"Normal"/"Restricted"
        dto.setDischargeDisposition(e.getDischargeDisposition()); // e.g., "Home", "Admitted"
        dto.setReasonForVisit(e.getReasonForVisit());             // e.g., "Chest pain"
        dto.setInCollection(e.getInCollection());                 // Boolean (or convert as needed)

        // Dates/timestamps — match your entity/DTO types (Instant/Long/LocalDateTime)
        dto.setEncounterDate(e.getEncounterDate());

        
        // Status (enum/string) — make sure entity has @Enumerated(EnumType.STRING) if DB stores text
        dto.setStatus(e.getStatus());

        // If you have more fields in DTO, map them here one-by-one.
        // dto.setVisitNumber(e.getVisitNumber());
        // dto.setAppointmentId(e.getAppointmentId());
        // dto.setLocationId(e.getLocationId());
        // dto.setProviderId(e.getProviderId());
        // ...
        return dto;
    }

}
