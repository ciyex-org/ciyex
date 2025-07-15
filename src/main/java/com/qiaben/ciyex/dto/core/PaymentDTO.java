package com.qiaben.ciyex.dto.core;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    @NotBlank(message = "Payment ID is required")
    private String id;

    @NotNull(message = "Bill date is required")
    private LocalDate billDate;
}
