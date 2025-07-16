package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.AppointmentDTO;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import com.qiaben.ciyex.mapper.AppointmentFhirMapper;
import com.qiaben.ciyex.service.fhir.OpenEmrFhirAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final OpenEmrFhirAppointmentService fhirAppointmentService;

    public ApiResponse<AppointmentResponseDTO> create(AppointmentDTO dto) {
        var fhirDto   = AppointmentFhirMapper.toFhir(dto);
        var response  = fhirAppointmentService.createAppointment(fhirDto);

        return ApiResponse.<AppointmentResponseDTO>builder()
                .success(true)
                .data(response)
                .message("Appointment created successfully")
                .build();
    }
}
