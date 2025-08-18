package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class SmtpConfig {
    private String server;
    private String username;
    private String password;
}
