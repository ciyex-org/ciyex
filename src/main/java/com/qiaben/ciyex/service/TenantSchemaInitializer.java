package com.qiaben.ciyex.service;

import com.qiaben.ciyex.migration.TenantFlywayMigrator;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
public class TenantSchemaInitializer {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TenantFlywayMigrator tenantFlywayMigrator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment env;
    
    // Cache to track initialized schemas
    private final ConcurrentHashMap<Long, Boolean> initializedSchemas = new ConcurrentHashMap<>();
    // Per-tenant locks to prevent concurrent initialization for the same org
    private final ConcurrentHashMap<Long, Object> tenantLocks = new ConcurrentHashMap<>();
    // Global semaphore to limit concurrent heavy tenant initialization work
    // Allows configuring concurrency via TENANT_INIT_CONCURRENCY env var (default 3)
    private final Semaphore initSemaphore;

    public TenantSchemaInitializer() {
        int concurrency = 3;
        try {
            String v = System.getenv("TENANT_INIT_CONCURRENCY");
            if (v != null && !v.isBlank()) concurrency = Integer.parseInt(v);
        } catch (Exception ignore) {}
        initSemaphore = new Semaphore(Math.max(1, concurrency));
    }
    
    // Test method to initialize a tenant schema for testing purposes
    @Transactional
    public void testTenantSchemaCreation() {
        log.info("Testing tenant schema creation for orgId: 1");
        initializeTenantSchema(1L);
        log.info("Test tenant schema creation completed");
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
    
    @Transactional
    public void initializeTenantSchema(Long orgId) {
        if (orgId == null) {
            return;
        }

        String schemaName = "practice_" + orgId;

        log.debug("initializeTenantSchema called for orgId: {} schema: {}", orgId, schemaName);
        // Limit overall concurrency for heavy tenant initialization work to avoid exhausting DB connections
        boolean permitAcquired = false;
        try {
            permitAcquired = initSemaphore.tryAcquire(30, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        if (!permitAcquired) {
            String msg = "Could not acquire tenant initialization permit within timeout - pool may be busy";
            log.warn(msg + " for orgId: {}", orgId);
            throw new RuntimeException(msg);
        }

        // Use a per-tenant lock so only one thread initializes a given tenant at a time.
        Object lock = tenantLocks.computeIfAbsent(orgId, k -> new Object());
        try {
            synchronized (lock) {
                // Double-check after acquiring the lock
                if (initializedSchemas.containsKey(orgId)) {
                    log.debug("Tenant schema already initialized for orgId: {}. Running pending Flyway migrations.", orgId);
                    try {
                        tenantFlywayMigrator.migrate(schemaName, orgId);
                    } catch (Exception e) {
                        log.warn("Flyway migrate failed while post-check for {}: {}", schemaName, e.getMessage());
                    }
                    return;
                }

                try {
                    // Create schema if it doesn't exist
                    createSchemaIfNotExists(schemaName);

                    // Create all tenant tables using Hibernate metadata
                    createTenantTablesFromEntities(schemaName);

                    // Ensure that if some tables are missing (schema existed previously) we create any missing ones
                    // This helps when entity set changed between deployments — compare and create missing tables
                    ensureTenantTablesExist(orgId);

                    // Apply tenant-specific Flyway migrations after tables exist
                    tenantFlywayMigrator.migrate(schemaName, orgId);

                    // Mark as initialized
                    initializedSchemas.put(orgId, true);

                    log.info("Successfully initialized tenant schema with all application tables: {}", schemaName);
                } catch (Exception e) {
                    // Detect common Hikari connection pool exhaust situation and provide extra context in logs
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("HikariPool") || msg.contains("Connection is not available")) {
                        log.error("Hikari pool timeout while initializing schema {}. Consider increasing Hikari pool size or avoiding concurrent initializations.", schemaName);
                    }
                    log.error("Failed to initialize tenant schema: {}", schemaName, e);
                    throw new RuntimeException("Failed to initialize tenant schema: " + schemaName, e);
                } finally {
                    // remove lock to avoid memory leak (only remove if this object is still mapped to this key)
                    tenantLocks.remove(orgId);
                }
            }
        } finally {
            // Always release the semaphore permit
            initSemaphore.release();
        }
    }

    // Run post-creation migrations for an existing tenant schema (safe, idempotent)
    @Transactional
    public void runTenantMigrations(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        try {
            // Use only the tenant schema in search_path
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();
            ensureJsonbColumn(schemaName, "org_config", "integrations");
            tenantFlywayMigrator.migrate(schemaName, orgId);
        } catch (Exception e) {
            log.warn("Tenant migration failed for schema {}: {}", schemaName, e.getMessage());
        } finally {
            try { entityManager.createNativeQuery("SET search_path TO public").executeUpdate(); } catch (Exception ignore) {}
        }
    }
    
    private void createSchemaIfNotExists(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE SCHEMA IF NOT EXISTS " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create schema: " + schemaName, ex);
        }
    }
    
    private void createTenantTablesFromEntities(String schemaName) {
        try {
            // Set search path to tenant schema only (not including public)
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();
            
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
    // Use configured datasource settings from Spring Environment (application.yml, env vars, CLI args, etc.)
    String jdbcUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/ciyexdb");
    String dbUser = env.getProperty("spring.datasource.username", "postgres");
    String dbPass = env.getProperty("spring.datasource.password", "postgres");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.url", jdbcUrl)
            .applySetting("hibernate.connection.username", dbUser)
            .applySetting("hibernate.connection.password", dbPass)
                    .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
                    .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                    .applySetting("hibernate.hbm2ddl.auto", "update")
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
            com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName),
            com.qiaben.ciyex.util.SqlIdentifier.quote(tableName),
            com.qiaben.ciyex.util.SqlIdentifier.quote(columnName),
            com.qiaben.ciyex.util.SqlIdentifier.quote(columnName));
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

            // Create a simple table structure dynamically based on entity fields
            StringBuilder ddl = new StringBuilder();
            ddl.append(String.format("CREATE TABLE IF NOT EXISTS %s.%s (",
                com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName),
                com.qiaben.ciyex.util.SqlIdentifier.quote(tableName)));

            // Always add an ID column (quote the identifier to avoid reserved-word issues)
            ddl.append(String.format("%s BIGSERIAL PRIMARY KEY", com.qiaben.ciyex.util.SqlIdentifier.quote("id")));

            // Add other columns based on entity fields (simplified approach)
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.getName().equals("id") && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    String columnName = camelToSnakeCase(field.getName());
                    String columnType = getColumnType(field.getType());
                    // Quote column names to handle reserved words (e.g., user) and special characters
                    ddl.append(String.format(", %s %s", com.qiaben.ciyex.util.SqlIdentifier.quote(columnName), columnType));
                }
            }

            ddl.append(")");

            entityManager.createNativeQuery(ddl.toString()).executeUpdate();
            log.debug("Created table: {}.{} for entity: {}", schemaName, tableName, entityClass.getSimpleName());

        } catch (Exception e) {
            log.warn("Failed to create table for entity {}: {}", entityClass.getSimpleName(), e.getMessage());
            // Fallback: create a basic table with just ID
            String fallbackDDL = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s BIGSERIAL PRIMARY KEY)",
                com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName),
                com.qiaben.ciyex.util.SqlIdentifier.quote(tableName),
                com.qiaben.ciyex.util.SqlIdentifier.quote("id"));
            entityManager.createNativeQuery(fallbackDDL).executeUpdate();
            log.debug("Created fallback table: {}.{}", schemaName, tableName);
        }
    }

    /**
     * Ensure all tenant tables exist for the given org schema. This compares the JPA tenant entities
     * to information_schema and creates any missing tables using existing helpers.
     */
    @Transactional
    public void ensureTenantTablesExist(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;

        try {
            // Ensure search_path to tenant schema
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();

            List<Class<?>> tenantEntities = getAllTenantEntities();
            log.info("Ensuring {} tenant entities exist in schema {}", tenantEntities.size(), schemaName);

            for (Class<?> entityClass : tenantEntities) {
                try {
                    String tableName = getTableName(entityClass);
                    String checkTableQuery = String.format(
                            "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                            schemaName, tableName
                    );
                    Object result = entityManager.createNativeQuery(checkTableQuery).getSingleResult();
                    boolean exists = false;
                    if (result instanceof Boolean) {
                        exists = (Boolean) result;
                    } else if (result instanceof Number) {
                        exists = ((Number) result).intValue() == 1;
                    } else if (result != null) {
                        exists = Boolean.parseBoolean(result.toString());
                    }

                    if (!exists) {
                        log.info("Table {}.{} missing - creating", schemaName, tableName);
                        createTableForEntity(entityClass, schemaName);
                    } else {
                        log.debug("Table {}.{} already exists", schemaName, tableName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to verify/create table for entity {}: {}", entityClass.getSimpleName(), e.getMessage());
                }
            }

            // Ensure known JSON columns
            ensureJsonbColumn(schemaName, "org_config", "integrations");

        } catch (Exception e) {
            log.warn("Failed to ensure tenant tables exist for schema {}: {}", schemaName, e.getMessage());
        } finally {
            try { entityManager.createNativeQuery("SET search_path TO public").executeUpdate(); } catch (Exception ignore) {}
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
