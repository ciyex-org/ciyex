package com.qiaben.ciyex.storage.fhir;


import com.qiaben.ciyex.entity.Vitals;
import com.qiaben.ciyex.storage.ExternalVitalsStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FhirExternalVitalsStorage implements ExternalVitalsStorage {

    @Override
    public void save(Vitals vitals) {
        // TODO: Map to FHIR Observation
        log.info("Pushing vitals {} to FHIR", vitals.getId());
    }

    @Override
    public void delete(Long id) {
        // TODO: Delete from FHIR
        log.info("Deleting vitals {} from FHIR", id);
    }

    @Override
    public byte[] print(Vitals vitals) {
        // TODO: Generate PDF
        log.info("Printing vitals {}", vitals.getId());
        return new byte[0];
    }
}
