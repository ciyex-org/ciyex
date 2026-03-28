package org.ciyex.ehr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class KeycloakConfig {
    
    @Value("${keycloak.enabled:false}")
    private boolean enabled;
    
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String resource;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    
    public String getTokenEndpoint() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }
    
    public String getUserInfoEndpoint() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
    }
    
    public String getLogoutEndpoint() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }
}
