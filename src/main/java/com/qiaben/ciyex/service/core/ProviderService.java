package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.storage.fhir.FhirProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {

    private final FhirProviderService fhirProviderService;

    /**
     * Registers a provider (practitioner) in OpenEMR FHIR.
     *
     * @param practitioner FHIR Practitioner resource from the client
     * @return ApiResponse containing Practitioner and status
     */
    public ApiResponse<Practitioner> registerProvider(Practitioner practitioner) {
        log.info("Registering provider: {}", practitioner.getId());

        try {
            // Register provider/practitioner in FHIR
            Practitioner createdPractitioner = fhirProviderService.createProvider(practitioner);

            log.info("Provider registered successfully: {}", createdPractitioner.getId());
            return ApiResponse.<Practitioner>builder()
                    .success(true)
                    .message("Provider registered successfully!")
                    .data(createdPractitioner)
                    .build();
        } catch (Exception e) {
            log.error("Failed to register provider", e);
            return ApiResponse.<Practitioner>builder()
                    .success(false)
                    .message("Failed to register provider: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Saves provider working schedule.
     *
     * @param workingDays List of working day objects (use your domain model or map directly)
     * @return ApiResponse with success status and saved data
     */
    public ApiResponse<List<Object>> saveSchedule(List<Object> workingDays) {
        log.info("Saving provider working schedule: {} days", workingDays.size());

        // TODO: Persist the workingDays in database if needed

        log.info("Working days saved successfully.");
        return ApiResponse.<List<Object>>builder()
                .success(true)
                .message("Working days saved successfully!")
                .data(workingDays)
                .build();
    }
}
