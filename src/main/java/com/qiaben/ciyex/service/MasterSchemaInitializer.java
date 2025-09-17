package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.service.TenantSchemaInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMasterSchema() {
        log.info("Initializing master schema with authentication tables (via JPA entities)...");

        try {
            // Ensure we're in the public schema
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();

            // Create master schema tables using JPA entities
            createMasterSchemaTables();

            log.info("Master schema initialization completed successfully");
            
            // After master schema is initialized, check and initialize tenant schemas
            initializeTenantSchemasForExistingOrgs();
            
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
            
            if (usersExists && orgsExists && userOrgRolesExists) {
                log.info("All master schema tables already exist. Skipping creation.");
                return;
            }
            
            log.info("Some master schema tables missing. Creating tables from JPA entities...");
            
            // Create a temporary Hibernate configuration for schema generation
            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySetting("hibernate.connection.url", "jdbc:postgresql://localhost:5432/ciyexdb")
                    .applySetting("hibernate.connection.username", "postgres")
                    .applySetting("hibernate.connection.password", "postgres")
                    .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
                    .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                    .applySetting("hibernate.hbm2ddl.auto", "create")
                    .applySetting("hibernate.default_schema", "public")
                    .applySetting("hibernate.show_sql", "true")
                    .build();

            // Build metadata from our master schema entities
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(Org.class)
                    .addAnnotatedClass(UserOrgRole.class)
                    .buildMetadata();

            // Create the schema using Hibernate's schema management tool
            SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
            
            log.info("Master schema tables created/updated successfully using JPA entities");
            
            // Verify tables were created
            sessionFactory.close();
            serviceRegistry.close();
            
            // Double-check that tables now exist
            if (tableExists("users") && tableExists("orgs") && tableExists("user_org_roles")) {
                log.info("Verified: All master schema tables created successfully");
            } else {
                log.warn("Warning: Some tables may not have been created properly");
            }
            
        } catch (Exception e) {
            log.error("Failed to create master schema tables using Hibernate metadata", e);
            throw new RuntimeException("Schema creation failed", e);
        }
    }
    
    private void initializeTenantSchemasForExistingOrgs() {
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
            
            for (Org org : allOrgs) {
                Long orgId = org.getId();
                String tenantSchemaName = "practice_" + orgId;
                
                // Check if tenant schema exists
                if (!tenantSchemaExists(tenantSchemaName)) {
                    log.info("Tenant schema {} not found for org ID {}. Initializing...", tenantSchemaName, orgId);
                    tenantSchemaInitializer.initializeTenantSchema(orgId);
                    log.info("Successfully initialized tenant schema for org ID {}", orgId);
                } else {
                    log.debug("Tenant schema {} already exists for org ID {}", tenantSchemaName, orgId);
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
}
