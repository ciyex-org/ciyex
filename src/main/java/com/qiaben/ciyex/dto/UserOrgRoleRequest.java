package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class UserOrgRoleRequest {
    private Long orgId;
    private String role; // must be one of ADMIN, PROVIDER, PATIENT, NURSE, etc.
}
