package com.qiaben.ciyex.dto.core.integration;

import lombok.Data;

@Data
public class OpenEmrConfig {
    private String apiUrl;
    private String clientId;
    private String tokenUrl;
    private String scope;
    private String audience;
    private String kid;
    private String privateKeyPath;
}
