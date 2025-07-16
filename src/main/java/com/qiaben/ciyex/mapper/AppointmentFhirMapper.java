package com.qiaben.ciyex.mapper;

import com.qiaben.ciyex.dto.core.AppointmentDTO;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppointmentFhirMapper {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_TIME;

    public static AppointmentResponseDTO toFhir(AppointmentDTO dto) {
        String start = LocalDate.parse(dto.getAppointmentDate(), DATE)
                .atTime(LocalTime.parse(dto.getTime(), TIME))
                .toString();        // 2025‑07‑16T09:00

        // assume 30‑min slot for demo; change as needed
        String end   = LocalDate.parse(dto.getAppointmentDate(), DATE)
                .atTime(LocalTime.parse(dto.getTime(), TIME).plusMinutes(30))
                .toString();

        return AppointmentResponseDTO.builder()
                .description(dto.getType())
                .start(start)
                .end(end)
                .participant(
                        AppointmentResponseDTO.Participant.builder()
                                .actorReference("Practitioner/" + dto.getDoctorId())
                                .actorDisplay("Doctor " + dto.getDoctorId())
                                .build()
                )
                .build();
    }
}
