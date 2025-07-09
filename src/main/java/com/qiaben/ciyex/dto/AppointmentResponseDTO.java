package com.qiaben.ciyex.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AppointmentResponseDTO {
    private String resourceType;
    private Meta meta;
    private List<Entry> entry; // List of entries, each representing an appointment

    @Data
    @NoArgsConstructor
    public static class Meta {
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class Entry {
        private AppointmentResponseDTO resource; // This will hold the appointment resource itself
    }
}
