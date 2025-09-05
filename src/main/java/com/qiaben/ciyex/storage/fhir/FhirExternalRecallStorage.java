package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.RecallDto;
import com.qiaben.ciyex.storage.ExternalRecallStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalRecallStorage")
@Slf4j
public class FhirExternalRecallStorage implements ExternalRecallStorage {

    @Override
    public String createRecall(RecallDto dto) {
        CommunicationRequest request = new CommunicationRequest();
        request.setStatus(CommunicationRequest.CommunicationRequestStatus.ACTIVE);
        request.setReasonCode(Collections.singletonList(
                new org.hl7.fhir.r4.model.CodeableConcept().setText(dto.getRecallReason())
        ));
        String externalId = UUID.randomUUID().toString();
        log.info("Generated externalId for Recall: {}", externalId);
        return externalId;
    }

    @Override
    public void updateRecall(RecallDto dto, String externalId) {
        log.info("Updating recall {} in FHIR", externalId);
    }

    @Override
    public RecallDto getRecall(String externalId) {
        RecallDto dto = new RecallDto();
        dto.setFhirId(externalId);
        return dto;
    }

    @Override
    public void deleteRecall(String externalId) {
        log.info("Deleting recall {} in FHIR", externalId);
    }

    @Override
    public List<RecallDto> searchAllRecalls() {
        return Collections.emptyList();
    }

    @Override
    public String create(RecallDto entityDto) { return createRecall(entityDto); }
    @Override
    public void update(RecallDto entityDto, String externalId) { updateRecall(entityDto, externalId); }
    @Override
    public RecallDto get(String externalId) { return getRecall(externalId); }
    @Override
    public void delete(String externalId) { deleteRecall(externalId); }
    @Override
    public List<RecallDto> searchAll() { return searchAllRecalls(); }
    @Override
    public boolean supports(Class<?> entityType) { return RecallDto.class.isAssignableFrom(entityType); }
}
