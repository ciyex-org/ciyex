package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaService {
    
    private final DataSource dataSource;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${keycloak.admin.username:aran-admin}")
    private String adminUsername;
    
    @Value("${keycloak.admin.password:}")
    private String adminPassword;
    
    @Value("${ciyex.tenant.auto-create-on-request:true}")
    private boolean autoCreateOnRequest;
    
    public void setTenantSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            setSchema(schemaName);
        } else {
            setSchema("public");
        }
    }
    
    private void setSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create schema if it doesn't exist
            if (!"public".equals(schemaName)) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                log.debug("Ensured schema exists: {}", schemaName);
            }
            
            // Set search path to use the tenant schema first, then public
            String searchPath = "public".equals(schemaName) ? "public" : com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public";
            statement.execute("SET search_path TO " + searchPath);
            
            log.debug("Set search_path to: {}", searchPath);
            
        } catch (SQLException e) {
            log.error("Failed to set schema: {}", schemaName, e);
            throw new RuntimeException("Failed to set tenant schema", e);
        }
    }
    
    public String getCurrentSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            return "practice_" + context.getOrgId();
        }
        return "public";
    }
    
    /**
     * Generate schema name from tenant group name
     * Example: "Qiaben Health" -> "qiaben_health"
     */
    public String generateSchemaName(String tenantGroupName) {
        if (tenantGroupName == null || tenantGroupName.isBlank()) {
            throw new IllegalArgumentException("Tenant group name cannot be null or empty");
        }
        
        return tenantGroupName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
    
    /**
     * Check if schema exists in database
     */
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
        
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, schemaName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check if schema exists: {}", schemaName, e);
        }
        
        return false;
    }
    
    /**
     * Create schema for tenant (migrations removed)
     */
    public void createSchema(String schemaName) {
        try {
            log.info("Creating schema for: {}", schemaName);
            
            // Create schema if it doesn't exist
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                log.info("Schema created: {}", schemaName);
            }
            
        } catch (Exception e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema", e);
        }
    }
    
    /**
     * Update Keycloak group with schema_name attribute
     */
    public void updateKeycloakGroupAttribute(String tenantGroupName, String schemaName) {
        try {
            log.info("Updating Keycloak group '{}' with schema_name: {}", tenantGroupName, schemaName);
            
            // Get admin token
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get Keycloak admin token");
                return;
            }
            
            // Find Tenants group
            String tenantsGroupId = findGroupId(adminToken, "Tenants");
            if (tenantsGroupId == null) {
                log.error("Tenants group not found in Keycloak");
                return;
            }
            
            // Find tenant subgroup
            String tenantGroupId = findSubGroupId(adminToken, tenantsGroupId, tenantGroupName);
            if (tenantGroupId == null) {
                log.error("Tenant group '{}' not found in Keycloak", tenantGroupName);
                return;
            }
            
            // Get current group data
            String groupUrl = String.format("%s/admin/realms/%s/groups/%s", 
                    keycloakUrl, keycloakRealm, tenantGroupId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.set("Content-Type", "application/json");
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    groupUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to get group data from Keycloak");
                return;
            }
            
            Map<String, Object> groupData = response.getBody();
            if (groupData == null) {
                log.error("Empty group data from Keycloak");
                return;
            }
            
            // Update attributes
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) groupData.getOrDefault("attributes", new HashMap<>());
            attributes.put("schema_name", new String[]{schemaName});
            groupData.put("attributes", attributes);
            
            // Remove fields that shouldn't be in update
            groupData.remove("subGroups");
            groupData.remove("realmRoles");
            groupData.remove("clientRoles");
            
            // Update group
            ResponseEntity<Void> updateResponse = restTemplate.exchange(
                    groupUrl, HttpMethod.PUT, new HttpEntity<>(groupData, headers), Void.class);
            
            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully updated Keycloak group '{}' with schema_name: {}", 
                        tenantGroupName, schemaName);
            } else {
                log.error("Failed to update Keycloak group: {}", updateResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error updating Keycloak group attribute", e);
        }
    }
    
    /**
     * Ensure schema exists for tenant, create if needed, and update Keycloak
     */
    public String ensureSchemaForTenant(String tenantGroupName) {
        String schemaName = generateSchemaName(tenantGroupName);
        
        if (!schemaExists(schemaName)) {
            if (autoCreateOnRequest) {
                log.info("Schema '{}' does not exist for tenant '{}', creating...", 
                        schemaName, tenantGroupName);
                
                createSchema(schemaName);
                updateKeycloakGroupAttribute(tenantGroupName, schemaName);
            } else {
                log.error("Schema '{}' does not exist for tenant '{}' and auto-create is disabled", 
                        schemaName, tenantGroupName);
                throw new RuntimeException(
                    String.format("Schema '%s' does not exist for tenant '%s'. " +
                        "Auto-creation is disabled. Please create the schema manually.", 
                        schemaName, tenantGroupName));
            }
        } else {
            log.debug("Schema '{}' already exists for tenant '{}'", schemaName, tenantGroupName);
        }
        
        return schemaName;
    }
    
    private String getAdminToken() {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                    keycloakUrl, keycloakRealm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            String body = String.format(
                    "client_id=admin-cli&username=%s&password=%s&grant_type=password",
                    adminUsername, adminPassword);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl, new HttpEntity<>(body, headers), Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to get admin token", e);
        }
        return null;
    }
    
    private String findGroupId(String token, String groupName) {
        try {
            String url = String.format("%s/admin/realms/%s/groups", keycloakUrl, keycloakRealm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> group : response.getBody()) {
                    if (groupName.equals(group.get("name"))) {
                        return (String) group.get("id");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find group: {}", groupName, e);
        }
        return null;
    }
    
    private String findSubGroupId(String token, String parentGroupId, String subGroupName) {
        try {
            String url = String.format("%s/admin/realms/%s/groups/%s/children", 
                    keycloakUrl, keycloakRealm, parentGroupId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> group : response.getBody()) {
                    if (subGroupName.equals(group.get("name"))) {
                        return (String) group.get("id");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find subgroup: {}", subGroupName, e);
        }
        return null;
    }
}
