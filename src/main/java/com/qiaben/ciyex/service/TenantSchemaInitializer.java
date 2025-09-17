package com.qiaben.ciyex.service;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
public class TenantSchemaInitializer {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    // Cache to track initialized schemas
    private final ConcurrentHashMap<Long, Boolean> initializedSchemas = new ConcurrentHashMap<>();
    
    // Test method to initialize a tenant schema for testing purposes
    @Transactional
    public void testTenantSchemaCreation() {
        log.info("Testing tenant schema creation for orgId: 1");
        initializeTenantSchema(1L);
        log.info("Test tenant schema creation completed");
    }
    
    // Method to test enhanced entity scanning with detailed logging
    @Transactional
    public void testEnhancedEntityScanning() {
        Long testOrgId = 99L;
        log.info("Testing enhanced entity scanning for orgId: {}", testOrgId);
        
        try {
            // Clear cache to ensure fresh creation
            initializedSchemas.remove(testOrgId);
            
            // Get all managed types and log them
            SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
            log.info("Scanning all JPA entities...");
            
            sessionFactory.getMetamodel().getManagedTypes().forEach(managedType -> {
                Class<?> entityClass = managedType.getJavaType();
                boolean isTenant = isTenantEntity(entityClass);
                log.info("Entity: {} - Is Tenant Entity: {} - Full Class Name: {}", 
                    entityClass.getSimpleName(), isTenant, entityClass.getName());
            });
            
            // Create tenant schema
            initializeTenantSchema(testOrgId);
            
            log.info("Enhanced entity scanning test completed for orgId: {}", testOrgId);
        } catch (Exception e) {
            log.error("Enhanced entity scanning test failed", e);
        }
    }
    
    @Transactional
    public void dropTenantSchema(Long orgId) {
        if (orgId == null) {
            return;
        }
        
        String schemaName = "practice_" + orgId;
        log.info("Dropping tenant schema: {}", schemaName);
        
        try {
            // Drop the entire schema and all its tables
            entityManager.createNativeQuery("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE").executeUpdate();
            
            // Remove from cache
            initializedSchemas.remove(orgId);
            
            log.info("Successfully dropped tenant schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to drop tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to drop tenant schema", e);
        }
    }
    
    public void initializeTenantSchema(Long orgId) {
        if (orgId == null) {
            return;
        }
        
        if (initializedSchemas.containsKey(orgId)) {
            log.debug("Tenant schema already initialized for orgId: {}", orgId);
            return;
        }

        String schemaName = "practice_" + orgId;
        
        try {
            // Create schema if it doesn't exist
            createSchemaIfNotExists(schemaName);
            
            // Create all tenant tables using Hibernate metadata
            createTenantTablesFromEntities(schemaName);
            
            // Mark as initialized
            initializedSchemas.put(orgId, true);
            
            log.info("Successfully initialized tenant schema with all application tables: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to initialize tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to initialize tenant schema: " + schemaName, e);
        }
    }

    // Run post-creation migrations for an existing tenant schema (safe, idempotent)
    @Transactional
    public void runTenantMigrations(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        try {
            // Use only the tenant schema in search_path
            entityManager.createNativeQuery("SET search_path TO " + schemaName).executeUpdate();
            ensureJsonbColumn(schemaName, "org_config", "integrations");
        } catch (Exception e) {
            log.warn("Tenant migration failed for schema {}: {}", schemaName, e.getMessage());
        } finally {
            try { entityManager.createNativeQuery("SET search_path TO public").executeUpdate(); } catch (Exception ignore) {}
        }
    }
    
    private void createSchemaIfNotExists(String schemaName) {
        entityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS " + schemaName).executeUpdate();
    }
    
    private void createTenantTablesFromEntities(String schemaName) {
        try {
            // Set search path to tenant schema only (not including public)
            entityManager.createNativeQuery("SET search_path TO " + schemaName).executeUpdate();
            
            // Get all tenant entities
            List<Class<?>> tenantEntities = getAllTenantEntities();
            log.info("Found {} tenant entities to create in schema: {}", tenantEntities.size(), schemaName);
            
            if (!tenantEntities.isEmpty()) {
                // Create all tenant tables using Hibernate schema generation
                createAllTenantTablesWithHibernate(tenantEntities, schemaName);
            }
            
            // Ensure known JSON columns use JSONB type (avoid 255-char limit)
            ensureJsonbColumn(schemaName, "org_config", "integrations");

            log.info("Created all tenant tables from JPA entities in schema: {}", schemaName);
            
        } catch (Exception e) {
            log.error("Failed to create tenant tables in schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant tables", e);
        } finally {
            // Reset search path to default (public schema)
            try {
                entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
                log.debug("Reset search path to public schema");
            } catch (Exception e) {
                log.warn("Failed to reset search path: {}", e.getMessage());
            }
        }
    }
    
    private List<Class<?>> getAllTenantEntities() {
        List<Class<?>> tenantEntities = new ArrayList<>();
        
        // Get the current SessionFactory to access Hibernate metadata
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        
        // Get all entity metadata from the current session factory
        sessionFactory.getMetamodel().getManagedTypes().forEach(managedType -> {
            Class<?> entityClass = managedType.getJavaType();
            log.debug("Processing entity: {} ({})", entityClass.getSimpleName(), entityClass.getName());
            
            if (isTenantEntity(entityClass)) {
                log.info("Adding TENANT entity: {}", entityClass.getSimpleName());
                tenantEntities.add(entityClass);
            } else {
                log.debug("Skipping MASTER entity: {} ({})", entityClass.getSimpleName(), entityClass.getName());
            }
        });
        
        return tenantEntities;
    }
    
    private void createAllTenantTablesWithHibernate(List<Class<?>> tenantEntities, String schemaName) {
        try {
            log.info("Creating {} tenant tables using Hibernate schema generation in schema: {}", 
                    tenantEntities.size(), schemaName);
            
            // Create a temporary Hibernate configuration for tenant schema generation
            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySetting("hibernate.connection.url", "jdbc:postgresql://localhost:5432/ciyexdb")
                    .applySetting("hibernate.connection.username", "postgres")
                    .applySetting("hibernate.connection.password", "postgres")
                    .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
                    .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                    .applySetting("hibernate.hbm2ddl.auto", "create")
                    .applySetting("hibernate.default_schema", schemaName)
                    .applySetting("hibernate.show_sql", "true")
                    .build();

            // Build metadata from tenant entities only
            MetadataSources metadataSources = new MetadataSources(serviceRegistry);
            for (Class<?> entityClass : tenantEntities) {
                log.debug("Adding entity to metadata: {}", entityClass.getSimpleName());
                metadataSources.addAnnotatedClass(entityClass);
            }
            
            Metadata metadata = metadataSources.buildMetadata();

            // Create the schema using Hibernate's schema management tool
            org.hibernate.SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
            
            log.info("Successfully created {} tenant tables using Hibernate schema generation", tenantEntities.size());
            
            sessionFactory.close();
            serviceRegistry.close();
            
        } catch (Exception e) {
            log.error("Failed to create tenant tables using Hibernate schema generation", e);
            // Fallback to individual table creation
            log.info("Falling back to individual table creation...");
            for (Class<?> entityClass : tenantEntities) {
                try {
                    createTableForEntity(entityClass, schemaName);
                } catch (Exception ex) {
                    log.warn("Failed to create table for entity {}: {}", entityClass.getSimpleName(), ex.getMessage());
                }
            }
        }
    }
    
    private void ensureJsonbColumn(String schemaName, String tableName, String columnName) {
        try {
            String columnTypeQuery = "SELECT data_type FROM information_schema.columns " +
                    "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            Object dataTypeObj = entityManager.createNativeQuery(columnTypeQuery)
                    .setParameter(1, schemaName)
                    .setParameter(2, tableName)
                    .setParameter(3, columnName)
                    .getSingleResult();

            String dataType = dataTypeObj != null ? dataTypeObj.toString() : null;
            if (dataType != null && !dataType.equalsIgnoreCase("jsonb")) {
                String alter = String.format(
                        "ALTER TABLE %s.%s ALTER COLUMN %s TYPE JSONB USING %s::jsonb",
                        schemaName, tableName, columnName, columnName);
                entityManager.createNativeQuery(alter).executeUpdate();
                log.info("Altered column {}.{}.{} to JSONB", schemaName, tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("Could not ensure JSONB type for {}.{}.{}: {}", schemaName, tableName, columnName, e.getMessage());
        }
    }
    
    private boolean isTenantEntity(Class<?> entityClass) {
        // Define master schema entities (authentication-related) - use full class names for accuracy
        Set<String> masterEntities = Set.of(
            "com.qiaben.ciyex.entity.User",
            "com.qiaben.ciyex.entity.Org", 
            "com.qiaben.ciyex.entity.UserOrgRole"
        );
        
        // Also check simple names as fallback
        Set<String> masterSimpleNames = Set.of(
            "User", "Org", "UserOrgRole"
        );
        
        // Only include entities that are JPA entities and not master entities
        boolean isJpaEntity = entityClass.isAnnotationPresent(Entity.class);
        boolean isMasterEntity = masterEntities.contains(entityClass.getName()) || 
                                masterSimpleNames.contains(entityClass.getSimpleName());
        
        log.debug("Entity check: {} - JPA: {}, Master: {}, Tenant: {}", 
                 entityClass.getName(), isJpaEntity, isMasterEntity, isJpaEntity && !isMasterEntity);
        
        return isJpaEntity && !isMasterEntity;
    }
    
    private void createTableForEntity(Class<?> entityClass, String schemaName) {
        // Use Spring Boot's existing DDL generation by temporarily switching schema context
        try {
            // Get table name from @Table annotation or use class name
            String tableName = getTableName(entityClass);
            
            // Check if table already exists
            String checkTableQuery = String.format(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                schemaName, tableName
            );
            
            Boolean tableExists = (Boolean) entityManager.createNativeQuery(checkTableQuery).getSingleResult();
            
            if (!tableExists) {
                // Create a temporary EntityManagerFactory for this schema to generate DDL
                createTableUsingHibernateDDL(entityClass, schemaName, tableName);
            }
            
        } catch (Exception e) {
            log.warn("Could not create table for {}: {}", entityClass.getSimpleName(), e.getMessage());
        }
    }
    
    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(jakarta.persistence.Table.class)) {
            jakarta.persistence.Table tableAnnotation = entityClass.getAnnotation(jakarta.persistence.Table.class);
            if (!tableAnnotation.name().isEmpty()) {
                return tableAnnotation.name();
            }
        }
        // Convert camelCase to snake_case
        return camelToSnakeCase(entityClass.getSimpleName());
    }
    
    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private void createTableUsingHibernateDDL(Class<?> entityClass, String schemaName, String tableName) {
        try {
            // Use Hibernate's SchemaExport to generate DDL for this specific entity
            SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
            
            // Create a simple table structure dynamically based on entity fields
            StringBuilder ddl = new StringBuilder();
            ddl.append(String.format("CREATE TABLE IF NOT EXISTS %s.%s (", schemaName, tableName));
            
            // Always add an ID column
            ddl.append("id BIGSERIAL PRIMARY KEY");
            
            // Add other columns based on entity fields (simplified approach)
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.getName().equals("id") && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    String columnName = camelToSnakeCase(field.getName());
                    String columnType = getColumnType(field.getType());
                    ddl.append(String.format(", %s %s", columnName, columnType));
                }
            }
            
            ddl.append(")");
            
            entityManager.createNativeQuery(ddl.toString()).executeUpdate();
            log.debug("Created table: {}.{} for entity: {}", schemaName, tableName, entityClass.getSimpleName());
            
        } catch (Exception e) {
            log.warn("Failed to create table for entity {}: {}", entityClass.getSimpleName(), e.getMessage());
            // Fallback: create a basic table with just ID
            String fallbackDDL = String.format("CREATE TABLE IF NOT EXISTS %s.%s (id BIGSERIAL PRIMARY KEY)", schemaName, tableName);
            entityManager.createNativeQuery(fallbackDDL).executeUpdate();
            log.debug("Created fallback table: {}.{}", schemaName, tableName);
        }
    }
    
    private String getColumnType(Class<?> fieldType) {
        // Common simple types
        if (fieldType == String.class) {
            return "VARCHAR(255)";
        } else if (fieldType == Long.class || fieldType == long.class) {
            return "BIGINT";
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return "INTEGER";
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "BOOLEAN";
        } else if (fieldType == java.time.LocalDate.class) {
            return "DATE";
        } else if (fieldType == java.time.LocalDateTime.class) {
            return "TIMESTAMP";
        } else if (fieldType == java.math.BigDecimal.class) {
            return "DECIMAL(19,2)";
        } else if (fieldType.isEnum()) {
            return "VARCHAR(50)";
        }

        // JSON payloads (e.g., OrgConfig.integrations) should not be limited to 255 chars
        // Map Jackson JsonNode to PostgreSQL JSONB for efficient storage and querying
        if ("com.fasterxml.jackson.databind.JsonNode".equals(fieldType.getName())) {
            return "JSONB";
        }

        // Default for other complex types
        return "VARCHAR(255)";
    }
}
