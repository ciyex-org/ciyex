package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.entity.AdminTemplate;
import com.qiaben.ciyex.repository.OrgRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.flywaydb.core.Flyway;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service to initialize master schema tables during startup.
 * Uses JPA entities to avoid hardcoded SQL - relies on Spring Boot's
 * automatic schema creation via hibernate.hbm2ddl.auto=update property.
 */
@Service
@Slf4j
public class MasterSchemaInitializer {

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private OrgRepository orgRepository;
    
    @Autowired
    private TenantSchemaInitializer tenantSchemaInitializer;

    @Autowired
    private Flyway masterFlyway;

    @Autowired
    private Environment env;

    private boolean tenantAutoInitEnabled = true;

    @PostConstruct
    public void init() {
        try {
            String prop = env.getProperty("ciyex.tenant.auto-init");
            if (prop != null && (prop.equalsIgnoreCase("false") || prop.equals("0"))) {
                this.tenantAutoInitEnabled = false;
            }
        } catch (Exception ignore) {}
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMasterSchema() {
        log.info("Initializing master schema with authentication tables (via JPA entities)...");

        try {
            // Ensure we're in the public schema
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();

            Set<Long> existingOrgIds = fetchExistingOrgIds();

            // Apply master schema Flyway migrations (data, seed scripts, etc.)
            // Run Flyway first so migrations create the canonical schema objects
            // before Hibernate attempts to create/update them. This avoids
            // "relation already exists" errors when both Flyway and Hibernate
            // try to create the same tables.
            masterFlyway.migrate();

            // Create master schema tables using JPA entities (idempotent check)
            createMasterSchemaTables();

            // Clear persistence context to ensure we see fresh data post-migration
            entityManager.clear();

            log.info("Master schema initialization completed successfully");
            
            // After master schema is initialized, optionally initialize tenant schemas
            if (tenantAutoInitEnabled) {
                initializeTenantSchemasForExistingOrgs(existingOrgIds);
            } else {
                log.info("Automatic tenant initialization is disabled (ciyex.tenant.auto-init=false). Skipping tenant schema initialization at startup.");
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize master schema", e);
            throw new RuntimeException("Master schema initialization failed", e);
        }
    }
    
    private void createMasterSchemaTables() {
        log.info("Creating master schema tables using Hibernate metadata...");

        try {
            // Check if tables already exist before creating
            boolean usersExists = tableExists("users");
            boolean orgsExists = tableExists("orgs");
            boolean userOrgRolesExists = tableExists("user_org_roles");
            boolean adminTemplatesExists = tableExists("admin_templates");
            boolean orgConfigExists = tableExists("org_config");

            // If all known master tables are present, skip creation
            if (usersExists && orgsExists && userOrgRolesExists && adminTemplatesExists && orgConfigExists) {
                log.info("All master schema tables already exist. Skipping creation.");
                return;
            }

            log.info("Some master schema tables missing. Creating tables from JPA entities...");

            // Create a temporary Hibernate configuration for schema generation
            // Prefer datasource settings from Spring Environment (which aggregates application.yml, env, CLI, etc.)
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
                    .applySetting("hibernate.default_schema", "public")
                    .applySetting("hibernate.show_sql", "true")
                    .build();

            // Build metadata from our master schema entities
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(Org.class)
                    .addAnnotatedClass(UserOrgRole.class)
                    .addAnnotatedClass(AdminTemplate.class)
                    .addAnnotatedClass(OrgConfig.class)  // Ensure OrgConfig is present in master schema
                    .buildMetadata();

            // Create the schema using Hibernate's schema management tool
            SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();

            log.info("Master schema tables created/updated successfully using JPA entities");

            // Ensure JSONB column for OrgConfig integrations in master schema
            ensureMasterJsonbColumn("public", "org_config", "integrations");

            // Verify tables were created
            sessionFactory.close();
            serviceRegistry.close();

            // Double-check that tables now exist
            if (tableExists("users") && tableExists("orgs") && tableExists("user_org_roles") && tableExists("admin_templates") && tableExists("org_config")) {
                log.info("Verified: All master schema tables created successfully");
            } else {
                log.warn("Warning: Some tables may not have been created properly");
            }

        } catch (Exception e) {
            log.error("Failed to create master schema tables using Hibernate metadata", e);
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    private void initializeTenantSchemasForExistingOrgs(Set<Long> orgIdsBeforeMigration) {
        log.info("Scanning orgs table to initialize missing tenant schemas...");
        
        try {
            // Ensure we're in the public schema to read orgs
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
            
            // Check if orgs table exists before trying to query it
            if (!tableExists("orgs")) {
                log.info("Orgs table does not exist yet. Skipping tenant schema initialization.");
                return;
            }
            
            // Get all organizations from the master schema
            List<Org> allOrgs = orgRepository.findAll();
            log.info("Found {} organizations in master schema", allOrgs.size());

            Set<Long> newOrgIds = new HashSet<>();
            if (orgIdsBeforeMigration != null && !orgIdsBeforeMigration.isEmpty()) {
                for (Org org : allOrgs) {
                    if (!orgIdsBeforeMigration.contains(org.getId())) {
                        newOrgIds.add(org.getId());
                    }
                }
            } else {
                allOrgs.forEach(org -> newOrgIds.add(org.getId()));
            }

            for (Org org : allOrgs) {
                Long orgId = org.getId();
                String tenantSchemaName = "practice_" + orgId;

                // Check if tenant schema exists
                if (!tenantSchemaExists(tenantSchemaName)) {
                    log.info("Tenant schema {} not found for org ID {}. Initializing...", tenantSchemaName, orgId);
                    tenantSchemaInitializer.initializeTenantSchema(orgId);
                    log.info("Successfully initialized tenant schema for org ID {}", orgId);
                } else {
                    if (newOrgIds.contains(orgId)) {
                        log.info("Schema {} already existed but org {} is new this run. Ensuring migrations are applied.", tenantSchemaName, orgId);
                    } else {
                        log.debug("Tenant schema {} already exists for org ID {}", tenantSchemaName, orgId);
                    }
                    // Ensure any missing tables are created (compare entities with information_schema)
                    try {
                        tenantSchemaInitializer.ensureTenantTablesExist(orgId);
                    } catch (Exception e) {
                        log.warn("Failed to ensure tenant tables for org {}: {}", orgId, e.getMessage());
                    }

                    // Run idempotent migrations to keep schema up to date (e.g., JSONB columns)
                    tenantSchemaInitializer.runTenantMigrations(orgId);
                }
            }
            
            log.info("Completed tenant schema initialization for all existing organizations");
            
        } catch (Exception e) {
            log.error("Failed to initialize tenant schemas for existing orgs", e);
            // Don't throw exception here - let the application continue even if tenant schema init fails
        }
    }
    
    private boolean tenantSchemaExists(String schemaName) {
        try {
            String checkSchemaQuery = "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
            Object result = entityManager.createNativeQuery(checkSchemaQuery)
                    .setParameter(1, schemaName)
                    .getSingleResult();
            return (Boolean) result;
        } catch (Exception e) {
            log.warn("Failed to check if schema {} exists: {}", schemaName, e.getMessage());
            return false;
        }
    }

    private Set<Long> fetchExistingOrgIds() {
        Set<Long> orgIds = new HashSet<>();
        try {
            if (!tableExists("orgs")) {
                return orgIds;
            }

            @SuppressWarnings("unchecked")
            List<Object> results = entityManager.createNativeQuery("SELECT id FROM orgs").getResultList();
            for (Object result : results) {
                if (result instanceof Number number) {
                    orgIds.add(number.longValue());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch existing org IDs: {}", e.getMessage());
        }
        return orgIds;
    }
    
    private boolean tableExists(String tableName) {
        try {
            String checkTableQuery = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)";
            Object result = entityManager.createNativeQuery(checkTableQuery)
                    .setParameter(1, tableName)
                    .getSingleResult();
            return (Boolean) result;
        } catch (Exception e) {
            log.warn("Failed to check if table {} exists: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Ensure a column in the master schema uses JSONB type (for PostgreSQL).
     */
    private void ensureMasterJsonbColumn(String schemaName, String tableName, String columnName) {
        try {
            // Check current data type
            String dataTypeQuery = "SELECT data_type FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            Object dataTypeObj = entityManager.createNativeQuery(dataTypeQuery)
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
                log.info("Altered column {}.{}.{} to JSONB in master schema", schemaName, tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("Could not ensure JSONB type for master schema column {}.{}.{}: {}", schemaName, tableName, columnName, e.getMessage());
        }
    }
}
