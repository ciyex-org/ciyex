package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PatientEducationDto;
import com.qiaben.ciyex.storage.ExternalPatientEducationStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Communication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalPatientEducationStorage")
@Slf4j
public class FhirExternalPatientEducationStorage implements ExternalPatientEducationStorage {

    @Override
    public String createEducation(PatientEducationDto dto) {
        Communication comm = new Communication();
        comm.setStatus(Communication.CommunicationStatus.COMPLETED);
        comm.addCategory(new org.hl7.fhir.r4.model.CodeableConcept()
                .setText("Language: " + dto.getLanguage()));
        String externalId = UUID.randomUUID().toString();
        log.info("Generated externalId for PatientEducation: {}", externalId);
        return externalId;
    }

    @Override
    public void updateEducation(PatientEducationDto dto, String externalId) {
        log.info("Updating patient education {} in FHIR", externalId);
    }

    @Override
    public PatientEducationDto getEducation(String externalId) {
        PatientEducationDto dto = new PatientEducationDto();
        dto.setFhirId(externalId);
        return dto;
    }

    @Override
    public void deleteEducation(String externalId) {
        log.info("Deleting patient education {} in FHIR", externalId);
    }

    @Override
    public List<PatientEducationDto> searchAllEducation() {
        return Collections.emptyList();
    }

    @Override
    public String create(PatientEducationDto entityDto) { return createEducation(entityDto); }
    @Override
    public void update(PatientEducationDto entityDto, String externalId) { updateEducation(entityDto, externalId); }
    @Override
    public PatientEducationDto get(String externalId) { return getEducation(externalId); }
    @Override
    public void delete(String externalId) { deleteEducation(externalId); }
    @Override
    public List<PatientEducationDto> searchAll() { return searchAllEducation(); }
    @Override
    public boolean supports(Class<?> entityType) { return PatientEducationDto.class.isAssignableFrom(entityType); }
}
