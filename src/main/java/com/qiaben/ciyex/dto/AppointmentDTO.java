package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {

    @NotBlank(message = "Select physician")
    private String doctorId;

    @NotBlank(message = "Select type of appointment")
    private String type;

    @NotBlank(message = "Select appointment date")
    private String appointmentDate;

    @NotBlank(message = "Select appointment time")
    private String time;

    @NotBlank(message = "Select appointment mode")
    private String mode;

    // Optional
    private String note;
}

