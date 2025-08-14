//package com.qiaben.ciyex.service;
//
//
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import com.qiaben.ciyex.entity.Immunization;
//import com.qiaben.ciyex.repository.ImmunizationRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class ImmunizationService {
//
//    private final ImmunizationRepository immunizationRepository;
//
//    public ImmunizationService(ImmunizationRepository immunizationRepository) {
//        this.immunizationRepository = immunizationRepository;
//    }
//
//    @Transactional
//    public ImmunizationDto create(ImmunizationDto dto) {
//        Immunization immunization = new Immunization();
//        immunization.setVaccineName(dto.getVaccineName());
//        immunization.setDateAdministered(dto.getDateAdministered());
//        immunization.setPatientId(dto.getPatientId());
//        immunization.setAdministeredBy(dto.getAdministeredBy());
//        immunization = immunizationRepository.save(immunization);
//        return mapToDto(immunization);
//    }
//
//    @Transactional(readOnly = true)
//    public ImmunizationDto getById(Long id) {
//        Immunization immunization = immunizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Immunization not found"));
//        return mapToDto(immunization);
//    }
//
//    @Transactional
//    public ImmunizationDto update(Long id, ImmunizationDto dto) {
//        Immunization immunization = immunizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Immunization not found"));
//        immunization.setVaccineName(dto.getVaccineName());
//        immunization.setDateAdministered(dto.getDateAdministered());
//        immunization.setAdministeredBy(dto.getAdministeredBy());
//        immunization = immunizationRepository.save(immunization);
//        return mapToDto(immunization);
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        Immunization immunization = immunizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Immunization not found"));
//        immunizationRepository.delete(immunization);
//    }
//
//    private ImmunizationDto mapToDto(Immunization immunization) {
//        ImmunizationDto dto = new ImmunizationDto();
//        dto.setId(immunization.getId());
//        dto.setVaccineName(immunization.getVaccineName());
//        dto.setDateAdministered(immunization.getDateAdministered());
//        dto.setPatientId(immunization.getPatientId());
//        dto.setAdministeredBy(immunization.getAdministeredBy());
//        return dto;
//    }
//}
//



package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.entity.Immunization;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.repository.ImmunizationRepository;
import com.qiaben.ciyex.repository.EncounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImmunizationService {
    private final ImmunizationRepository immunizationRepository;
    private final EncounterRepository encounterRepository;

    public ImmunizationService(ImmunizationRepository immunizationRepository, EncounterRepository encounterRepository) {
        this.immunizationRepository = immunizationRepository;
        this.encounterRepository = encounterRepository;
    }

    @Transactional
    public ImmunizationDto create(Long encounterId, ImmunizationDto dto) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + encounterId));
        Immunization immunization = mapToEntity(dto);
        immunization.setEncounter(encounter); // Set the encounter
        immunization = immunizationRepository.save(immunization);
        return mapToDto(immunization);
    }

    @Transactional(readOnly = true)
    public List<ImmunizationDto> getByEncounterId(Long encounterId) {
        List<Immunization> immunizations = immunizationRepository.findByEncounter_Id(encounterId);
        return immunizations.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ImmunizationDto update(Long encounterId, Long id, ImmunizationDto dto) {
        Immunization immunization = immunizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Immunization not found"));
        if (!immunization.getEncounter().getId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }
        immunization.setVaccineName(dto.getVaccineName());
        immunization.setDateAdministered(dto.getDateAdministered());
        immunization.setPatientId(dto.getPatientId());
        immunization.setAdministeredBy(dto.getAdministeredBy());
        immunization.setOrgId(dto.getOrgId());
      //  immunization.setImmuid(dto.getImmuid());
        immunization.setExternaleId(dto.getExternaleId());
        immunization = immunizationRepository.save(immunization);
        return mapToDto(immunization);
    }

    @Transactional
    public void delete(Long encounterId, Long id) {
        Immunization immunization = immunizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Immunization not found"));
        if (!immunization.getEncounter().getId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }
        immunizationRepository.delete(immunization);
    }

    private Immunization mapToEntity(ImmunizationDto dto) {
        Immunization immunization = new Immunization();
        immunization.setId(dto.getId());
        immunization.setVaccineName(dto.getVaccineName());
        immunization.setDateAdministered(dto.getDateAdministered());
        immunization.setPatientId(dto.getPatientId());
        immunization.setAdministeredBy(dto.getAdministeredBy());
        immunization.setOrgId(dto.getOrgId());
       // immunization.setImmuid(dto.getImmuid());
        immunization.setExternaleId(dto.getExternaleId());
        // encounter is set in create method
        return immunization;
    }

    private ImmunizationDto mapToDto(Immunization immunization) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setId(immunization.getId());
        dto.setVaccineName(immunization.getVaccineName());
        dto.setDateAdministered(immunization.getDateAdministered());
        dto.setPatientId(immunization.getPatientId());
        dto.setAdministeredBy(immunization.getAdministeredBy());
        dto.setEncounterId(immunization.getEncounter() != null ? immunization.getEncounter().getId() : null);
      dto.setOrgId(immunization.getOrgId());
       // dto.setImmuid(immunization.getImmuid());
        dto.setExternaleId(immunization.getExternaleId());
        return dto;
    }
}


//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import com.qiaben.ciyex.entity.Immunization;
//import com.qiaben.ciyex.entity.Encounter;
//import com.qiaben.ciyex.repository.ImmunizationRepository;
//import com.qiaben.ciyex.repository.EncounterRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ImmunizationService {
//
//    private final ImmunizationRepository immunizationRepository;
//    private final EncounterRepository encounterRepository;
//
//    public ImmunizationService(ImmunizationRepository immunizationRepository, EncounterRepository encounterRepository) {
//        this.immunizationRepository = immunizationRepository;
//        this.encounterRepository = encounterRepository;
//    }
//
//    @Transactional
//    public ImmunizationDto create(Long orgId, Long encounterId, ImmunizationDto dto) {
//        Encounter encounter = encounterRepository.findById(encounterId)
//                .orElseThrow(() -> new RuntimeException("Encounter not found with id: " + encounterId));
//
//        // Ensure orgId is set in the DTO
//        dto.setOrgId(orgId);
//        System.out.println("Received orgId: " + orgId);
//
//
//        Immunization immunization = mapToEntity(dto);
//
//        immunization.setEncounter(encounter); // Set the encounter
//        immunization = immunizationRepository.save(immunization);
//        return mapToDto(immunization);
//    }
//
//
//    @Transactional(readOnly = true)
//    public List<ImmunizationDto> getByEncounterId(Long orgId, Long encounterId) {
//        // Filter immunizations by orgId and encounterId
//        List<Immunization> immunizations = immunizationRepository.findByEncounter_Id(encounterId);
//
//        // Optionally filter by orgId if required in your logic
//        return immunizations.stream().map(this::mapToDto).collect(Collectors.toList());
//    }
//
//    @Transactional
//    public ImmunizationDto update(Long orgId, Long encounterId, Long id, ImmunizationDto dto) {
//        Immunization immunization = immunizationRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Immunization not found"));
//
//        if (!immunization.getEncounter().getId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        // Ensure orgId is set properly
//        immunization.setOrgId(orgId);
//
//        immunization.setVaccineName(dto.getVaccineName());
//        immunization.setDateAdministered(dto.getDateAdministered());
//        immunization.setPatientId(dto.getPatientId());
//        immunization.setAdministeredBy(dto.getAdministeredBy());
//        immunization.setExternaleId(dto.getExternaleId());
//        immunization = immunizationRepository.save(immunization);
//        return mapToDto(immunization);
//    }
//
//    @Transactional
//    public void delete(Long orgId, Long encounterId, Long id) {
//        Immunization immunization = immunizationRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Immunization not found"));
//
//        if (!immunization.getEncounter().getId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        // You may want to ensure that the immunization belongs to the given orgId before deletion
//        if (!immunization.getOrgId().equals(orgId)) {
//            throw new RuntimeException("Organization ID mismatch");
//        }
//
//        immunizationRepository.delete(immunization);
//    }
//
//    private Immunization mapToEntity(ImmunizationDto dto) {
//        Immunization immunization = new Immunization();
//        immunization.setId(dto.getId());
//        immunization.setVaccineName(dto.getVaccineName());
//        immunization.setDateAdministered(dto.getDateAdministered());
//        immunization.setPatientId(dto.getPatientId());
//        immunization.setAdministeredBy(dto.getAdministeredBy());
//        immunization.setOrgId(dto.getOrgId());
//        immunization.setExternaleId(dto.getExternaleId());
//        return immunization;
//    }
//
//    private ImmunizationDto mapToDto(Immunization immunization) {
//        ImmunizationDto dto = new ImmunizationDto();
//        dto.setId(immunization.getId());
//        dto.setVaccineName(immunization.getVaccineName());
//        dto.setDateAdministered(immunization.getDateAdministered());
//        dto.setPatientId(immunization.getPatientId());
//        dto.setAdministeredBy(immunization.getAdministeredBy());
//        dto.setEncounterId(immunization.getEncounter() != null ? immunization.getEncounter().getId() : null);
//        dto.setOrgId(immunization.getOrgId());
//        dto.setExternaleId(immunization.getExternaleId());
//        return dto;
//    }
//}
