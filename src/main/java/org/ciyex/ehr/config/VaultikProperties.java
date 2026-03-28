package org.ciyex.ehr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ciyex.vaultik")
@Data
public class VaultikProperties {
    private String localStoragePath = "/var/ciyex/files";
}
