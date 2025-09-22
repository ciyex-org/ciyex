package com.qiaben.ciyex.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/schema-verification")
public class SchemaVerificationController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/check-patient-schemas")
    public ResponseEntity<Map<String, Object>> checkPatientSchemas() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> schemaData = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Check patients in public schema
            String publicQuery = "SELECT 'public' as schema_name, COUNT(*) as patient_count, " +
                               "ARRAY_AGG(DISTINCT org_id) as org_ids FROM public.patients";
            try (PreparedStatement stmt = connection.prepareStatement(publicQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> publicData = new HashMap<>();
                    publicData.put("schema", "public");
                    publicData.put("patient_count", rs.getInt("patient_count"));
                    publicData.put("org_ids", rs.getArray("org_ids") != null ? 
                        rs.getArray("org_ids").getArray() : new Object[0]);
                    schemaData.add(publicData);
                }
            }
            
            // Check for tenant schemas
            String[] tenantSchemas = {"practice_1", "practice_2", "practice_3"};
            for (String schema : tenantSchemas) {
                String tenantQuery = "SELECT COUNT(*) as patient_count, " +
                                   "ARRAY_AGG(DISTINCT org_id) as org_ids FROM " + schema + ".patients";
                try (PreparedStatement stmt = connection.prepareStatement(tenantQuery);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> tenantData = new HashMap<>();
                        tenantData.put("schema", schema);
                        tenantData.put("patient_count", rs.getInt("patient_count"));
                        tenantData.put("org_ids", rs.getArray("org_ids") != null ? 
                            rs.getArray("org_ids").getArray() : new Object[0]);
                        schemaData.add(tenantData);
                    }
                } catch (SQLException e) {
                    // Schema might not exist or table might not exist
                    Map<String, Object> tenantData = new HashMap<>();
                    tenantData.put("schema", schema);
                    tenantData.put("patient_count", 0);
                    tenantData.put("org_ids", new Object[0]);
                    tenantData.put("error", "Schema or table does not exist");
                    schemaData.add(tenantData);
                }
            }
            
            result.put("success", true);
            result.put("schemas", schemaData);
            
        } catch (SQLException e) {
            log.error("Error checking patient schemas", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/list-schemas")
    public ResponseEntity<Map<String, Object>> listSchemas() {
        Map<String, Object> result = new HashMap<>();
        List<String> schemas = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'practice_%' OR schema_name = 'public'";
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString("schema_name"));
                }
            }
            
            result.put("success", true);
            result.put("schemas", schemas);
            
        } catch (SQLException e) {
            log.error("Error listing schemas", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
