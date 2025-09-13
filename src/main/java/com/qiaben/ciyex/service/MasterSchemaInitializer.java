package com.qiaben.ciyex.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to initialize only master schema tables during startup.
 * Only creates tables needed for user authentication and org identification.
 */
@Service
@Slf4j
public class MasterSchemaInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMasterSchema() {
        log.info("Initializing master schema with authentication tables only...");
        
        try {
            // Ensure we're in the public schema
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
            
            // Create only master schema tables for authentication and org mapping
            createUsersTable();
            createOrgsTable();
            createUserOrgRolesTable();
            
            log.info("Master schema initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize master schema", e);
            throw new RuntimeException("Master schema initialization failed", e);
        }
    }

    private void createUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGSERIAL PRIMARY KEY,
                uuid VARCHAR(255) UNIQUE,
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                first_name VARCHAR(255),
                last_name VARCHAR(255),
                middle_name VARCHAR(255),
                phone_number VARCHAR(255),
                date_of_birth VARCHAR(255),
                street VARCHAR(255),
                street2 VARCHAR(255),
                city VARCHAR(255),
                state VARCHAR(255),
                postal_code VARCHAR(255),
                country VARCHAR(255),
                profile_image VARCHAR(255),
                security_question VARCHAR(255),
                security_answer VARCHAR(255)
            )
            """;
        
        entityManager.createNativeQuery(sql).executeUpdate();
        log.info("Created users table in master schema");
    }

    private void createOrgsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS orgs (
                id BIGSERIAL PRIMARY KEY,
                org_name VARCHAR(255) NOT NULL,
                address VARCHAR(255),
                city VARCHAR(255),
                state VARCHAR(255),
                postal_code VARCHAR(255),
                country VARCHAR(255),
                fhir_id VARCHAR(255)
            )
            """;
        
        entityManager.createNativeQuery(sql).executeUpdate();
        log.info("Created orgs table in master schema");
    }

    private void createUserOrgRolesTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_org_roles (
                id BIGSERIAL PRIMARY KEY,
                user_id BIGINT NOT NULL,
                org_id BIGINT NOT NULL,
                role VARCHAR(255) NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (org_id) REFERENCES orgs(id),
                UNIQUE(user_id, org_id, role)
            )
            """;
        
        entityManager.createNativeQuery(sql).executeUpdate();
        log.info("Created user_org_roles table in master schema");
    }
}
