package org.ciyex.ehr.usermgmt.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String roleName;
    private Boolean enabled;
}
