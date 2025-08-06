package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String email;
    private String currentPassword;
    private String newPassword;
}
