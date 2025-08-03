package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDTO {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Service price is required")
    private String price;

    @NotBlank(message = "Service description is required")
    private String description;

    // Optional: If null, treated as not set; if false, must have all location fields
    private Boolean isOnline;

    private String hospitalName;
    private String address;
    private String city;
    private String state;
    private String zipCode;

    private String status;
    private String mode;
    private String category;

    private List<String> insuranceAccepted;

    /**
     * Custom validation: If isOnline is Boolean.FALSE, all location fields must be provided.
     */
    @AssertTrue(message = "All location fields are required for in-person services")
    public boolean isLocationValid() {
        if (Boolean.FALSE.equals(isOnline)) {
            return isNonEmpty(hospitalName)
                    && isNonEmpty(address)
                    && isNonEmpty(city)
                    && isNonEmpty(state)
                    && isNonEmpty(zipCode);
        }
        return true;
    }

    private boolean isNonEmpty(String val) {
        return val != null && !val.trim().isEmpty();
    }
}
