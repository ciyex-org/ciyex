package com.qiaben.ciyex.service.core;


import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.ProviderDTO;

import com.qiaben.ciyex.dto.core.WorkingDayDTO;
import com.qiaben.ciyex.dto.fhir.FhirProviderReportDTO;
import com.qiaben.ciyex.mapper.ProviderFhirMapper;
//import com.qiaben.ciyex.repository.ProviderServiceRepository;
import com.qiaben.ciyex.service.fhir.FhirProviderService;
import com.qiaben.ciyex.service.fhir.OpenEmrAuthService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Builder

public class ProviderService {

    private static FhirProviderService fhirProviderService;
    private static OpenEmrAuthService openEmrAuthService;
    public static ProviderServiceService ProviderServiceService;

    /**
     * Registers a provider (practitioner) in OpenEMR FHIR.
     *
     * @param providerDTO Provider DTO from the client
     * @return ApiResponse<FhirProviderReportDTO>
     */
    public static ApiResponse<?> registerProvider(ProviderDTO providerDTO) {
        // 1. Map ProviderDTO to FhirProviderReportDTO
        FhirProviderReportDTO fhirProviderReportDTO = ProviderFhirMapper.fromProviderDTO(providerDTO);

        try {
            // 2. Get OpenEMR access token
            String token = openEmrAuthService.getCachedAccessToken();

            // 3. Register provider/practitioner in FHIR
            FhirProviderReportDTO response;
            response = fhirProviderService.createProvider(fhirProviderReportDTO, token);

            // 4. Return wrapped API response
            return ApiResponse.<FhirProviderReportDTO>builder()
                    .success(true)
                    .message("Provider registered successfully!")
                    .data(response)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirProviderReportDTO>builder()
                    .success(false)
                    .message("Failed to register provider: " + e.getMessage())
                    .build();
        }
    }
    public static ApiResponse<List<WorkingDayDTO>> saveSchedule(List<WorkingDayDTO> workingDays) {
        // If you need to persist, do it here.
        // Example: providerScheduleRepository.saveAll(mappedEntities);

        // For now, just wrap and return as a success response
        return ApiResponse.<List<WorkingDayDTO>>builder()
                .success(true)
                .message("Working days saved successfully!")
                .data(workingDays)
                .build();
    }

}

