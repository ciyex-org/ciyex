package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.storage.ExternalMedicationRequestStorage;
import com.qiaben.ciyex.provider.FhirClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component("fhirExternalMedicationRequestStorage")
@Slf4j
public class FhirExternalMedicationRequestStorage implements ExternalMedicationRequestStorage {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public FhirExternalMedicationRequestStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalMedicationRequestStorage with FhirClientProvider");
    }

    @Override
    public String create(MedicationRequestDto dto) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            MedicationRequest fhirMedicationRequest = mapToFhirMedicationRequest(dto);
            String externalId = client.create().resource(fhirMedicationRequest).execute().getId().getIdPart();
            log.info("Successfully created MedicationRequest in FHIR with externalId: {}", externalId);
            return externalId;
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during create operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void update(MedicationRequestDto dto, String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            MedicationRequest fhirMedicationRequest = mapToFhirMedicationRequest(dto);
            fhirMedicationRequest.setId(externalId);
            client.update().resource(fhirMedicationRequest).execute();
            log.info("Successfully updated MedicationRequest in FHIR with externalId: {}", externalId);
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during update operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public MedicationRequestDto get(String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            MedicationRequest fhirMedicationRequest = client.read().resource(MedicationRequest.class).withId(externalId).execute();
            return mapFromFhirMedicationRequest(fhirMedicationRequest);
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during get operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void delete(String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById("MedicationRequest", externalId).execute();
            log.info("Successfully deleted MedicationRequest from FHIR with externalId: {}", externalId);
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during delete operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public List<MedicationRequestDto> searchAll() {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Bundle bundle = client.search().forResource(MedicationRequest.class).returnBundle(Bundle.class).execute();
            return bundle.getEntry().stream()
                    .map(entry -> {
                        MedicationRequest fhirMedicationRequest = (MedicationRequest) entry.getResource();
                        return mapFromFhirMedicationRequest(fhirMedicationRequest);
                    })
                    .collect(Collectors.toList());
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during searchAll operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    private MedicationRequest mapToFhirMedicationRequest(MedicationRequestDto dto) {
        MedicationRequest fhirMedicationRequest = new MedicationRequest();

        fhirMedicationRequest.setMedication(new CodeableConcept().setText(dto.getMedicationName()));

        // Set the Dosage field as a List
        Dosage dosage = new Dosage();
        dosage.setText(dto.getInstructions());
        fhirMedicationRequest.setDosageInstruction(List.of(dosage));

        fhirMedicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.fromCode(dto.getStatus()));

        try {
            fhirMedicationRequest.setAuthoredOn(DATE_FORMAT.parse(dto.getDateIssued()));
        } catch (ParseException e) {
            log.error("Failed to parse date: {}", dto.getDateIssued());
        }

        return fhirMedicationRequest;
    }

    private MedicationRequestDto mapFromFhirMedicationRequest(MedicationRequest fhirMedicationRequest) {
        MedicationRequestDto dto = new MedicationRequestDto();

        CodeableConcept medicationConcept = (CodeableConcept) fhirMedicationRequest.getMedication();
        if (medicationConcept != null) {
            dto.setMedicationName(medicationConcept.getText());
        }

        if (!fhirMedicationRequest.getDosageInstruction().isEmpty()) {
            Dosage dosage = fhirMedicationRequest.getDosageInstructionFirstRep();
            dto.setInstructions(dosage.getText());
        }

        dto.setStatus(fhirMedicationRequest.getStatus().toCode());

        if (fhirMedicationRequest.getAuthoredOn() != null) {
            dto.setDateIssued(DATE_FORMAT.format(fhirMedicationRequest.getAuthoredOn()));
        }

        return dto;
    }
}
