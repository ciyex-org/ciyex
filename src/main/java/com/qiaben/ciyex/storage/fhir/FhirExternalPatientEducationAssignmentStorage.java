package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.storage.ExternalPatientEducationAssignmentStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Communication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalPatientEducationAssignmentStorage")
@Slf4j
public class FhirExternalPatientEducationAssignmentStorage implements ExternalPatientEducationAssignmentStorage {

    @Override
    public String createAssignment(PatientEducationAssignmentDto dto) {
        Communication comm = new Communication();
        comm.setStatus(Communication.CommunicationStatus.INPROGRESS);
        comm.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/" + dto.getPatientId()));
        comm.setReasonCode(Collections.singletonList(
                new org.hl7.fhir.r4.model.CodeableConcept()
                        .setText(dto.getTopic() != null ? dto.getTopic().getTitle() : null)
        ));
        String externalId = UUID.randomUUID().toString();
        log.info("Generated externalId for PatientEducationAssignment: {}", externalId);
        return externalId;
    }

    @Override
    public void updateAssignment(PatientEducationAssignmentDto dto, String externalId) {
        log.info("Updating PatientEducationAssignment {} in FHIR", externalId);
    }

    @Override
    public PatientEducationAssignmentDto getAssignment(String externalId) {
        PatientEducationAssignmentDto dto = new PatientEducationAssignmentDto();

        PatientEducationAssignmentDto.TopicDto topic = new PatientEducationAssignmentDto.TopicDto();
        topic.setFhirId(externalId);

        dto.setTopic(topic);
        return dto;
    }


    @Override
    public void deleteAssignment(String externalId) {
        log.info("Deleting PatientEducationAssignment {} in FHIR", externalId);
    }

    @Override
    public List<PatientEducationAssignmentDto> searchAllAssignments() {
        return Collections.emptyList();
    }

    // generic interface methods
    @Override public String create(PatientEducationAssignmentDto dto) { return createAssignment(dto); }
    @Override public void update(PatientEducationAssignmentDto dto, String externalId) { updateAssignment(dto, externalId); }
    @Override public PatientEducationAssignmentDto get(String externalId) { return getAssignment(externalId); }
    @Override public void delete(String externalId) { deleteAssignment(externalId); }
    @Override public List<PatientEducationAssignmentDto> searchAll() { return searchAllAssignments(); }
    @Override public boolean supports(Class<?> entityType) { return PatientEducationAssignmentDto.class.isAssignableFrom(entityType); }
}
