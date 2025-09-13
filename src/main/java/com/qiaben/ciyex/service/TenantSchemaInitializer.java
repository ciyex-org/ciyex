package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TenantSchemaInitializer {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // Cache to track which schemas have been initialized
    private final ConcurrentHashMap<String, Boolean> initializedSchemas = new ConcurrentHashMap<>();
    
    public void initializeTenantSchema(Long orgId) {
        if (orgId == null) {
            return;
        }
        
        String schemaName = "practice_" + orgId;
        
        // Check if schema is already initialized
        if (initializedSchemas.containsKey(schemaName)) {
            return;
        }
        
        try {
            // Create schema if it doesn't exist
            entityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS " + schemaName).executeUpdate();
            
            // Create tables using reflection on JPA entities
            createTablesInTenantSchema(schemaName);
            
            // Mark schema as initialized
            initializedSchemas.put(schemaName, true);
            
            log.info("Successfully initialized tenant schema: {}", schemaName);
            
        } catch (Exception e) {
            log.error("Failed to initialize tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to initialize tenant schema: " + schemaName, e);
        }
    }
    
    private void createTablesInTenantSchema(String schemaName) {
        try {
            // Set search path to tenant schema first
            entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
            
            // Generate CREATE TABLE statement using reflection on Patient entity
            String createTableDDL = generateTableDDLFromEntity(Patient.class, schemaName);
            log.info("Generated DDL for schema {}: {}", schemaName, createTableDDL);
            entityManager.createNativeQuery(createTableDDL).executeUpdate();
            
            log.info("Created tables in tenant schema using JPA entity reflection: {}", schemaName);
            
        } catch (Exception e) {
            log.error("Failed to create tables in tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tables in tenant schema", e);
        }
    }
    
    private String generateTableDDLFromEntity(Class<?> entityClass, String schemaName) {
        // Get table name from @Table annotation or use class name
        String tableName = "patients"; // Default for Patient entity
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (!tableAnnotation.name().isEmpty()) {
                tableName = tableAnnotation.name();
            }
        }
        
        StringBuilder ddl = new StringBuilder();
        ddl.append(String.format("CREATE TABLE IF NOT EXISTS %s.%s (\n", schemaName, tableName));
        
        Field[] fields = entityClass.getDeclaredFields();
        boolean firstField = true;
        
        for (Field field : fields) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue; // Skip transient fields
            }
            
            if (!firstField) {
                ddl.append(",\n");
            }
            firstField = false;
            
            String columnName = getColumnName(field);
            String columnType = getColumnType(field);
            
            // Handle auto-generated ID fields specially
            if (field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(GeneratedValue.class) && columnType.equals("BIGINT")) {
                ddl.append("    ").append(columnName).append(" BIGSERIAL PRIMARY KEY");
            } else {
                ddl.append("    ").append(columnName).append(" ").append(columnType);
                
                // Add PRIMARY KEY constraint for @Id fields
                if (field.isAnnotationPresent(Id.class)) {
                    ddl.append(" PRIMARY KEY");
                }
            }
        }
        
        ddl.append("\n)");
        
        return ddl.toString();
    }
    
    private String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            if (!column.name().isEmpty()) {
                return column.name();
            }
        }
        // Convert camelCase to snake_case
        return camelToSnakeCase(field.getName());
    }
    
    private String getColumnType(Field field) {
        Class<?> fieldType = field.getType();
        
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            if (column.length() != 255) { // Custom length specified
                if (fieldType == String.class) {
                    return "VARCHAR(" + column.length() + ")";
                }
            }
        }
        
        // Map Java types to PostgreSQL types
        if (fieldType == String.class) {
            return "VARCHAR(255)";
        } else if (fieldType == Long.class || fieldType == long.class) {
            return "BIGINT";
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return "INTEGER";
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "BOOLEAN";
        } else if (fieldType == java.time.LocalDateTime.class || fieldType == java.util.Date.class) {
            return "TIMESTAMP";
        } else if (fieldType == java.time.LocalDate.class) {
            return "DATE";
        } else {
            return "VARCHAR(255)"; // Default fallback
        }
    }
    
    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    public void ensureTenantSchemaInitialized() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            initializeTenantSchema(context.getOrgId());
        }
    }
}
