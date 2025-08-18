package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.storage.ExternalHistoryOfPresentIllnessStorage;
import org.hl7.fhir.r4.model.Observation;  // We will use FHIR's Observation resource as an example
import org.springframework.stereotype.Component;

@Component
public class FhirExternalHistoryOfPresentIllnessStorage implements ExternalHistoryOfPresentIllnessStorage {

    @Override
    public void saveHistoryOfPresentIllness(HistoryOfPresentIllnessDto historyOfPresentIllnessDto) {
        // Convert the HistoryOfPresentIllnessDto to a FHIR Observation resource
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new org.hl7.fhir.r4.model.CodeableConcept().setText(historyOfPresentIllnessDto.getDescription()));
        observation.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/" + historyOfPresentIllnessDto.getPatientId()));
        observation.setEncounter(new org.hl7.fhir.r4.model.Reference("Encounter/" + historyOfPresentIllnessDto.getEncounterId()));

        // Send to the FHIR server (pseudo-code)
        // fhirClient.save(observation); // FHIR client to save the observation

        // For demo purposes, we'll just print out the details
        System.out.println("Saving HPI to FHIR: " + observation.getCode().getText());
    }

    @Override
    public HistoryOfPresentIllnessDto getHistoryOfPresentIllnessById(Long id) {
        // Fetch the History of Present Illness data from the FHIR system by ID (pseudo-code)
        // Observation observation = fhirClient.getObservationById(id);  // FHIR client to fetch observation

        // For demo purposes, we return mock data
        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();
        dto.setId(id);
        dto.setDescription("Neck pain with severe headache");
        dto.setPatientId(12345L);
        dto.setEncounterId(1L);
        dto.setOrgId(1L);
        dto.setCreatedAt(java.time.LocalDateTime.now());
        dto.setUpdatedAt(java.time.LocalDateTime.now());
        return dto;
    }
}
