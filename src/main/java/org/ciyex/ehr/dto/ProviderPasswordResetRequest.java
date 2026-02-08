package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class ProviderPasswordResetRequest {
    private String newPassword;
}
