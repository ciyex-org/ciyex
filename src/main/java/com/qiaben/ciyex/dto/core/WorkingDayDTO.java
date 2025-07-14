package com.qiaben.ciyex.dto.core;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingDayDTO {

    @NotNull(message = "Day is required")
    private DayOfWeek day;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "Close time is required")
    private String closeTime;

    public enum DayOfWeek {
        monday, tuesday, wednesday, thursday, friday, saturday, sunday
    }
}
