package org.ciyex.ehr.lab.service;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.lab.dto.LabOrderSetDto;
import org.ciyex.ehr.lab.entity.LabOrderSet;
import org.ciyex.ehr.lab.repository.LabOrderSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabOrderSetService {

    private final LabOrderSetRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional(readOnly = true)
    public List<LabOrderSetDto> getAll() {
        return repo.findByOrgAliasInAndActiveTrue(List.of(orgAlias(), "__GLOBAL__"))
                .stream().map(this::toDto).toList();
    }

    private LabOrderSetDto toDto(LabOrderSet e) {
        return LabOrderSetDto.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .description(e.getDescription())
                .tests(e.getTests())
                .category(e.getCategory())
                .active(e.getActive())
                .build();
    }
}
