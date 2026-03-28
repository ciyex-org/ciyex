package org.ciyex.ehr.dto.integration;

import lombok.Data;

@Data
public class SmtpConfig {
    private String server;
    private Integer port;       // nullable, safe if not provided in DB
    private String username;
    private String password;
    private String fromAddress; // optional, can be null
    private String fromName;    // optional, can be null
}
