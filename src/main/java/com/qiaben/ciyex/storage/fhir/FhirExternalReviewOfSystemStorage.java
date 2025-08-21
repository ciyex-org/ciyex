package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalReviewOfSystemStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalReviewOfSystemStorage implements ExternalReviewOfSystemStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(ReviewOfSystemDto dto) {
        log.info("FHIR ROS create org={} patient={} enc={} system={}",
                dto.getOrgId(), dto.getPatientId(), dto.getEncounterId(), dto.getSystemName());
        // Map one system to an Observation or QR item group
        return null;
    }

    @Override public void update(String externalId, ReviewOfSystemDto dto) { }
    @Override public Optional<ReviewOfSystemDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<ReviewOfSystemDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }
    @Override public List<ReviewOfSystemDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
