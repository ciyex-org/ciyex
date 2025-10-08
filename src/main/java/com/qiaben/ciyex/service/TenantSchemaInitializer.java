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
import jakarta.annotation.PostConstruct;
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

    private boolean tenantAutoInitEnabled;
    private final ConcurrentHashMap<Long, Boolean> initializedSchemas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> tenantLocks = new ConcurrentHashMap<>();
    private Semaphore initSemaphore;

    @PostConstruct
    public void init() {
        int concurrency = 3;
        try {
            String v = env.getProperty("TENANT_INIT_CONCURRENCY");
            if (v != null && !v.isBlank()) concurrency = Integer.parseInt(v);
        } catch (Exception ignore) {}
        initSemaphore = new Semaphore(Math.max(1, concurrency));

        boolean enabled = true;
        try {
            String prop = env.getProperty("ciyex.tenant.auto-init");
            if (prop != null && (prop.equalsIgnoreCase("false") || prop.equals("0"))) {
                enabled = false;
            }
        } catch (Exception ignore) {}
        this.tenantAutoInitEnabled = enabled;
    }

    @Transactional
    public void testTenantSchemaCreation() {
        log.info("Testing tenant schema creation for orgId: 1");
        initializeTenantSchema(1L);
        log.info("Test tenant schema creation completed");
    }

    @Transactional
    public void dropTenantSchema(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        log.info("Dropping tenant schema: {}", schemaName);

        try {
            entityManager.createNativeQuery("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE").executeUpdate();
            initializedSchemas.remove(orgId);
            log.info("Successfully dropped tenant schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to drop tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to drop tenant schema", e);
        }
    }

    @Transactional
    public void initializeTenantSchema(Long orgId) {
        if (orgId == null) return;

        if (!tenantAutoInitEnabled) {
            log.info("Automatic tenant schema initialization is disabled (ciyex.tenant.auto-init=false). Skipping init for orgId={}", orgId);
            return;
        }

        String schemaName = "practice_" + orgId;
        log.debug("initializeTenantSchema called for orgId: {} schema: {}", orgId, schemaName);

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

        Object lock = tenantLocks.computeIfAbsent(orgId, k -> new Object());
        try {
            synchronized (lock) {
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
                    createSchemaIfNotExists(schemaName);
                    createTenantTablesFromEntities(schemaName);
                    ensureTenantTablesExist(orgId);
                    tenantFlywayMigrator.migrate(schemaName, orgId);
                    initializedSchemas.put(orgId, true);
                    log.info("Successfully initialized tenant schema with all application tables: {}", schemaName);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("HikariPool") || msg.contains("Connection is not available")) {
                        log.error("Hikari pool timeout while initializing schema {}.", schemaName);
                    }
                    log.error("Failed to initialize tenant schema: {}", schemaName, e);
                    throw new RuntimeException("Failed to initialize tenant schema: " + schemaName, e);
                } finally {
                    tenantLocks.remove(orgId);
                }
            }
        } finally {
            initSemaphore.release();
        }
    }

    @Transactional
    public void runTenantMigrations(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        try {
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();
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
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();
            List<Class<?>> tenantEntities = getAllTenantEntities();
            log.info("Found {} tenant entities to create in schema: {}", tenantEntities.size(), schemaName);
            if (!tenantEntities.isEmpty()) {
                createAllTenantTablesWithHibernate(tenantEntities, schemaName);
            }
            log.info("Created all tenant tables from JPA entities in schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to create tenant tables in schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant tables", e);
        } finally {
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
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        sessionFactory.getMetamodel().getManagedTypes().forEach(managedType -> {
            Class<?> entityClass = managedType.getJavaType();
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

            MetadataSources metadataSources = new MetadataSources(serviceRegistry);
            for (Class<?> entityClass : tenantEntities) {
                metadataSources.addAnnotatedClass(entityClass);
            }

            Metadata metadata = metadataSources.buildMetadata();
            org.hibernate.SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
            sessionFactory.close();
            serviceRegistry.close();

            log.info("Successfully created {} tenant tables using Hibernate schema generation", tenantEntities.size());
        } catch (Exception e) {
            log.error("Failed to create tenant tables using Hibernate schema generation", e);
        }
    }

    private boolean isTenantEntity(Class<?> entityClass) {
        Set<String> masterEntities = Set.of(
                "com.qiaben.ciyex.entity.User",
                "com.qiaben.ciyex.entity.Org",
                "com.qiaben.ciyex.entity.UserOrgRole",
                "com.qiaben.ciyex.entity.AdminTemplate",
                "com.qiaben.ciyex.entity.OrgConfig",
                "com.qiaben.ciyex.entity.GpsBillingCard",

                "com.qiaben.ciyex.entity.GpsPayment",
                "com.qiaben.ciyex.entity.InvoiceBill",
                "com.qiaben.ciyex.entity.StripeBillingCard",

                "com.qiaben.ciyex.entity.Subscription",
                "com.qiaben.ciyex.entity.ServiceEntity",
                // ✅ Added to master list so it’s excluded from tenant schemas
                "com.qiaben.ciyex.entity.BillingHistory"
        );

        Set<String> masterSimpleNames = Set.of(
                "User", "Org", "UserOrgRole", "AdminTemplate", "OrgConfig",
                "GpsBillingCard",  "GpsPayment", "InvoiceBill",
                "StripeBillingCard",  "Subscription", "ServiceEntity",
                // ✅ Added here too
                "BillingHistory"
        );

        boolean isJpaEntity = entityClass.isAnnotationPresent(Entity.class);
        boolean isMasterEntity = masterEntities.contains(entityClass.getName()) ||
                masterSimpleNames.contains(entityClass.getSimpleName());

        return isJpaEntity && !isMasterEntity;
    }

    @Transactional
    public void ensureTenantTablesExist(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;

        try {
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName)).executeUpdate();
            List<Class<?>> tenantEntities = getAllTenantEntities();
            for (Class<?> entityClass : tenantEntities) {
                try {
                    String tableName = getTableName(entityClass);
                    String checkTableQuery = String.format(
                            "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                            schemaName, tableName
                    );
                    Object result = entityManager.createNativeQuery(checkTableQuery).getSingleResult();
                    boolean exists = (result instanceof Boolean && (Boolean) result) ||
                            (result instanceof Number && ((Number) result).intValue() == 1);
                    if (!exists) {
                        log.info("Table {}.{} missing - creating", schemaName, tableName);
                        createTableForEntity(entityClass, schemaName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to verify/create table for entity {}: {}", entityClass.getSimpleName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to ensure tenant tables exist for schema {}: {}", schemaName, e.getMessage());
        } finally {
            try { entityManager.createNativeQuery("SET search_path TO public").executeUpdate(); } catch (Exception ignore) {}
        }
    }

    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(jakarta.persistence.Table.class)) {
            jakarta.persistence.Table tableAnnotation = entityClass.getAnnotation(jakarta.persistence.Table.class);
            if (!tableAnnotation.name().isEmpty()) {
                return tableAnnotation.name();
            }
        }
        return camelToSnakeCase(entityClass.getSimpleName());
    }

    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private void createTableForEntity(Class<?> entityClass, String schemaName) {
        try {
            String tableName = getTableName(entityClass);
            String checkTableQuery = String.format(
                    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                    schemaName, tableName
            );
            Boolean tableExists = (Boolean) entityManager.createNativeQuery(checkTableQuery).getSingleResult();
            if (!tableExists) {
                createTableUsingHibernateDDL(entityClass, schemaName, tableName);
            }
        } catch (Exception e) {
            log.warn("Could not create table for {}: {}", entityClass.getSimpleName(), e.getMessage());
        }
    }

    private void createTableUsingHibernateDDL(Class<?> entityClass, String schemaName, String tableName) {
        try {
            StringBuilder ddl = new StringBuilder();
            ddl.append(String.format("CREATE TABLE IF NOT EXISTS %s.%s (",
                    com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName),
                    com.qiaben.ciyex.util.SqlIdentifier.quote(tableName)));
            ddl.append(String.format("%s BIGSERIAL PRIMARY KEY", com.qiaben.ciyex.util.SqlIdentifier.quote("id")));
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.getName().equals("id") && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    String columnName = camelToSnakeCase(field.getName());
                    String columnType = getColumnType(field.getType());
                    ddl.append(String.format(", %s %s", com.qiaben.ciyex.util.SqlIdentifier.quote(columnName), columnType));
                }
            }
            ddl.append(")");
            entityManager.createNativeQuery(ddl.toString()).executeUpdate();
        } catch (Exception e) {
            log.warn("Failed to create table for entity {}: {}", entityClass.getSimpleName(), e.getMessage());
            String fallbackDDL = String.format("CREATE TABLE IF NOT EXISTS %s.%s (%s BIGSERIAL PRIMARY KEY)",
                    com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName),
                    com.qiaben.ciyex.util.SqlIdentifier.quote(tableName),
                    com.qiaben.ciyex.util.SqlIdentifier.quote("id"));
            entityManager.createNativeQuery(fallbackDDL).executeUpdate();
        }
    }

    private String getColumnType(Class<?> fieldType) {
        if (fieldType == String.class) return "VARCHAR(255)";
        else if (fieldType == Long.class || fieldType == long.class) return "BIGINT";
        else if (fieldType == Integer.class || fieldType == int.class) return "INTEGER";
        else if (fieldType == Boolean.class || fieldType == boolean.class) return "BOOLEAN";
        else if (fieldType == java.time.LocalDate.class) return "DATE";
        else if (fieldType == java.time.LocalDateTime.class) return "TIMESTAMP";
        else if (fieldType == java.math.BigDecimal.class) return "DECIMAL(19,2)";
        else if (fieldType.isEnum()) return "VARCHAR(50)";
        if ("com.fasterxml.jackson.databind.JsonNode".equals(fieldType.getName())) return "JSONB";
        return "VARCHAR(255)";
    }
}
