package com.qiaben.ciyex.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Single schema configuration for one practice per instance.
 * Sets the default schema at application startup based on CIYEX_SCHEMA_NAME environment variable.
 */
@Slf4j
@Configuration
public class SingleSchemaConfig {

    @Value("${ciyex.schema.name:public}")
    private String schemaName;

    @Bean
    public CommandLineRunner initializeSchema(DataSource dataSource) {
        return args -> {
            log.info("Initializing single schema configuration: {}", schemaName);
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Create schema if it doesn't exist (for non-public schemas)
                if (!"public".equals(schemaName)) {
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + 
                            com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
                    log.info("Ensured schema exists: {}", schemaName);
                }
                
                // Set default search path for this instance
                statement.execute("ALTER DATABASE ciyexdb SET search_path TO " + 
                        com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                
                log.info("Successfully configured instance to use schema: {}", schemaName);
                log.info("All database operations will use this schema by default");
                
            } catch (Exception e) {
                log.error("Failed to initialize schema configuration", e);
                throw new RuntimeException("Failed to initialize single schema configuration", e);
            }
        };
    }
}
