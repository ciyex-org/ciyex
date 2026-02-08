package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.EncounterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FHIR-only Encounter Browser Service.
 * Delegates to EncounterService for FHIR data access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterBrowserService {

    private final EncounterService encounterService;

    public Page<EncounterDto> listAll(Optional<String> statusOpt,
                                      boolean recentOnly,
                                      int recentCount,
                                      Pageable pageable
    ) {
        // Get all encounters from FHIR
        List<EncounterDto> allEncounters = encounterService.getAllEncounters();

        // Filter by status if provided
        if (statusOpt.isPresent()) {
            String status = statusOpt.get();
            allEncounters = allEncounters.stream()
                    .filter(e -> status.equalsIgnoreCase(e.getStatus() != null ? e.getStatus().toString() : null))
                    .collect(Collectors.toList());
        }

        // Sort by encounter date descending
        allEncounters.sort(Comparator.comparing(
                EncounterDto::getEncounterDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // Apply pagination
        int start;
        int end;
        int pageSize;

        if (recentOnly) {
            start = 0;
            pageSize = Math.max(1, recentCount);
            end = Math.min(pageSize, allEncounters.size());
        } else {
            pageSize = pageable.getPageSize();
            start = (int) pageable.getOffset();
            end = Math.min(start + pageSize, allEncounters.size());
        }

        if (start > allEncounters.size()) {
            return new PageImpl<>(List.of(), pageable, allEncounters.size());
        }

        List<EncounterDto> pageContent = allEncounters.subList(start, end);
        return new PageImpl<>(pageContent, PageRequest.of(start / pageSize, pageSize), allEncounters.size());
    }

    public List<EncounterDto> getAllEncounters() {
        return encounterService.getAllEncounters();
    }
}