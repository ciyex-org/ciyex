package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class GpsConfig {
    private String username;
    private String password;
    private String securityKey;
    private String collectjsPublicKey;
    private String transactUrl;
    private String webhookUrl;
}