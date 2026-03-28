package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvSupplierDto {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Boolean active;

    private String createdAt;
    private String updatedAt;
}
