package com.qiaben.ciyex.dto.fhir;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDTO{
    private String resourceType = "Appointment";
    private String status      = "booked";      // FHIR‑required
    private String description;                 // e.g. “Consultation”
    private String start;                       // ISO 8601 date‑time
    private String end;                         // ISO 8601 date‑time
    private Participant participant;            // physician + patient

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Participant {
        private String actorReference;          // “Practitioner/{id}”
        private String actorDisplay;            // Doctor name
        private boolean required = true;
        private String status    = "accepted";
    }
}
