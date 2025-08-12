package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import com.qiaben.ciyex.repository.HistoryOfPresentIllnessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryOfPresentIllnessService {

    private final HistoryOfPresentIllnessRepository historyOfPresentIllnessRepository;

    public HistoryOfPresentIllnessService(HistoryOfPresentIllnessRepository historyOfPresentIllnessRepository) {
        this.historyOfPresentIllnessRepository = historyOfPresentIllnessRepository;
    }

    // Create a new History of Present Illness entry
    @Transactional
    public HistoryOfPresentIllnessDto create(HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllness hpi = new HistoryOfPresentIllness();
        hpi.setDescription(dto.getDescription());
        hpi.setPatientId(dto.getPatientId());
        hpi.setEncounterId(dto.getEncounterId());
        hpi.setOrgId(dto.getOrgId());  // Set orgId
        hpi = historyOfPresentIllnessRepository.save(hpi);
        return mapToDto(hpi);
    }

    // Get all History of Present Illness entries for a specific encounter
    @Transactional(readOnly = true)
    public List<HistoryOfPresentIllnessDto> getByEncounterId(Long encounterId) {
        List<HistoryOfPresentIllness> hpis = historyOfPresentIllnessRepository.findByEncounterId(encounterId);
        return hpis.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Update a specific History of Present Illness entry
    @Transactional
    public HistoryOfPresentIllnessDto update(Long encounterId, Long id, HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllness hpi = historyOfPresentIllnessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("History of Present Illness not found"));
        if (!hpi.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }
        hpi.setDescription(dto.getDescription());
        hpi.setPatientId(dto.getPatientId());
        hpi.setOrgId(dto.getOrgId());  // Update orgId
        hpi = historyOfPresentIllnessRepository.save(hpi);
        return mapToDto(hpi);
    }

    // Delete a specific History of Present Illness entry
    @Transactional
    public void delete(Long encounterId, Long id) {
        HistoryOfPresentIllness hpi = historyOfPresentIllnessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("History of Present Illness not found"));
        if (!hpi.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }
        historyOfPresentIllnessRepository.delete(hpi);
    }

    private HistoryOfPresentIllnessDto mapToDto(HistoryOfPresentIllness hpi) {
        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();
        dto.setId(hpi.getId());
        dto.setDescription(hpi.getDescription());
        dto.setPatientId(hpi.getPatientId());
        dto.setEncounterId(hpi.getEncounterId());
        dto.setOrgId(hpi.getOrgId());
        dto.setCreatedAt(hpi.getCreatedAt());
        dto.setUpdatedAt(hpi.getUpdatedAt());
        return dto;
    }
}
