package org.ciyex.ehr.usermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResetPasswordResponse {
    private String userId;
    private String username;
    private String temporaryPassword;
    private String practiceName;
    private String portalUrl;
    private String resetDate;
}
