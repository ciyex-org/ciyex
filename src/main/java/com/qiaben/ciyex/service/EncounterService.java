//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.EncounterDto;
//import com.qiaben.ciyex.entity.Encounter;
//import com.qiaben.ciyex.repository.EncounterRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class EncounterService {
//
//    private final EncounterRepository encounterRepository;
//
//    @Autowired
//    public EncounterService(EncounterRepository encounterRepository) {
//        this.encounterRepository = encounterRepository;
//    }
//
//    @Transactional
//    public EncounterDto createEncounter(EncounterDto encounterDto) {
//        Encounter encounter = mapToEntity(encounterDto);
//        encounter.setCreatedAt(System.currentTimeMillis());
//        encounter.setUpdatedAt(System.currentTimeMillis());
//        encounter = encounterRepository.save(encounter);
//        return mapToDto(encounter);
//    }
//
//    public EncounterDto getEncounterById(Long id) {
//        Encounter encounter = encounterRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));
//        return mapToDto(encounter);
//    }
//
//    // Method to update encounter
//    public EncounterDto updateEncounter(Long id, EncounterDto encounterDto) {
//        Encounter encounter = encounterRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));
//
//        // Update logic based on DTO values
//        encounter.setVisitCategory(encounterDto.getVisitCategory());
//        encounter.setEncounterProvider(encounterDto.getEncounterProvider());
//        encounter.setType(encounterDto.getType());
//        encounter.setSensitivity(encounterDto.getSensitivity());
//        encounter.setDischargeDisposition(encounterDto.getDischargeDisposition());
//        encounter.setReasonForVisit(encounterDto.getReasonForVisit());
//        encounter.setInCollection(encounterDto.getInCollection());
//        encounter.setUpdatedAt(System.currentTimeMillis());
//
//        encounter = encounterRepository.save(encounter);
//        return mapToDto(encounter);
//    }
//
//    private Encounter mapToEntity(EncounterDto dto) {
//        Encounter encounter = new Encounter();
//        encounter.setVisitCategory(dto.getVisitCategory());
//        encounter.setEncounterProvider(dto.getEncounterProvider());
//        encounter.setType(dto.getType());
//        encounter.setSensitivity(dto.getSensitivity());
//        encounter.setDischargeDisposition(dto.getDischargeDisposition());
//        encounter.setReasonForVisit(dto.getReasonForVisit());
//        encounter.setCreatedAt(dto.getCreatedAt());
//        encounter.setUpdatedAt(dto.getUpdatedAt());
//        return encounter;
//    }
//
//    private EncounterDto mapToDto(Encounter encounter) {
//        EncounterDto dto = new EncounterDto();
//        dto.setId(encounter.getId());
//        dto.setVisitCategory(encounter.getVisitCategory());
//        dto.setEncounterProvider(encounter.getEncounterProvider());
//        dto.setType(encounter.getType());
//        dto.setSensitivity(encounter.getSensitivity());
//        dto.setDischargeDisposition(encounter.getDischargeDisposition());
//        dto.setReasonForVisit(encounter.getReasonForVisit());
//        dto.setCreatedAt(encounter.getCreatedAt());
//        dto.setUpdatedAt(encounter.getUpdatedAt());
//        return dto;
//    }
//}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.repository.EncounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;

    @Autowired
    public EncounterService(EncounterRepository encounterRepository) {
        this.encounterRepository = encounterRepository;
    }

    // Create Encounter
    public EncounterDto createEncounter(EncounterDto encounterDto, Long orgId) {
        Encounter encounter = mapToEntity(encounterDto);
        encounter.setOrgId(orgId);  // Set orgId for multi-tenancy
        encounter = encounterRepository.save(encounter);
        return mapToDto(encounter);
    }

    // Get Encounter by ID
    public EncounterDto getEncounterById(Long id, Long orgId) {
        Encounter encounter = encounterRepository.findById(id)
                .filter(enc -> enc.getOrgId().equals(orgId)) // Ensure orgId matches
                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));
        return mapToDto(encounter);
    }

    // Update Encounter
    public EncounterDto updateEncounter(Long id, EncounterDto encounterDto, Long orgId) {
        Encounter encounter = encounterRepository.findById(id)
                .filter(enc -> enc.getOrgId().equals(orgId)) // Ensure orgId matches
                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + id));

        encounter.setVisitCategory(encounterDto.getVisitCategory());
        encounter.setEncounterProvider(encounterDto.getEncounterProvider());
        encounter.setType(encounterDto.getType());
        encounter.setSensitivity(encounterDto.getSensitivity());
        encounter.setDischargeDisposition(encounterDto.getDischargeDisposition());
        encounter.setReasonForVisit(encounterDto.getReasonForVisit());
        encounter.setInCollection(encounterDto.getInCollection());
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
        encounter.setOrgId(dto.getOrgId());  // Set orgId
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
        dto.setOrgId(encounter.getOrgId());  // Map orgId to DTO
        return dto;
    }
}

