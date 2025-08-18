package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.repository.EncounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;

    @Autowired
    public EncounterService(EncounterRepository encounterRepository) {
        this.encounterRepository = encounterRepository;
    }

    @Transactional
    public EncounterDto createEncounter(EncounterDto encounterDto) {
        Encounter encounter = mapToEntity(encounterDto);
        encounter.setCreatedAt(System.currentTimeMillis());
        encounter.setUpdatedAt(System.currentTimeMillis());
        encounter = encounterRepository.save(encounter);
        return mapToDto(encounter);
    }

    public EncounterDto getEncounterById(Long id) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));
        return mapToDto(encounter);
    }

    // Method to update encounter
    public EncounterDto updateEncounter(Long id, EncounterDto encounterDto) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));

        // Update logic based on DTO values
        encounter.setVisitCategory(encounterDto.getVisitCategory());
        encounter.setEncounterProvider(encounterDto.getEncounterProvider());
        encounter.setType(encounterDto.getType());
        encounter.setSensitivity(encounterDto.getSensitivity());
        encounter.setDischargeDisposition(encounterDto.getDischargeDisposition());
        encounter.setReasonForVisit(encounterDto.getReasonForVisit());
        encounter.setInCollection(encounterDto.getInCollection());
        encounter.setUpdatedAt(System.currentTimeMillis());

        encounter = encounterRepository.save(encounter);
        return mapToDto(encounter);
    }

    private Encounter mapToEntity(EncounterDto dto) {
        Encounter encounter = new Encounter();
        encounter.setVisitCategory(dto.getVisitCategory());
        encounter.setEncounterProvider(dto.getEncounterProvider());
        encounter.setType(dto.getType());
        encounter.setSensitivity(dto.getSensitivity());
        encounter.setDischargeDisposition(dto.getDischargeDisposition());
        encounter.setReasonForVisit(dto.getReasonForVisit());
        encounter.setCreatedAt(dto.getCreatedAt());
        encounter.setUpdatedAt(dto.getUpdatedAt());
        return encounter;
    }

    private EncounterDto mapToDto(Encounter encounter) {
        EncounterDto dto = new EncounterDto();
        dto.setId(encounter.getId());
        dto.setVisitCategory(encounter.getVisitCategory());
        dto.setEncounterProvider(encounter.getEncounterProvider());
        dto.setType(encounter.getType());
        dto.setSensitivity(encounter.getSensitivity());
        dto.setDischargeDisposition(encounter.getDischargeDisposition());
        dto.setReasonForVisit(encounter.getReasonForVisit());
        dto.setCreatedAt(encounter.getCreatedAt());
        dto.setUpdatedAt(encounter.getUpdatedAt());
        return dto;
    }
}
