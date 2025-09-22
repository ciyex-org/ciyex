package com.qiaben.ciyex.config;

import com.qiaben.ciyex.migration.TenantFlywayMigrator;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class FlywayConfig {

    private final ResourceLoader resourceLoader;

    public FlywayConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Flyway masterFlyway(DataSource dataSource,
                               @Value("${ciyex.env:local}") String environment) {
        String[] locations = resolveFlywayLocations("db/migration/master", environment);
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations(locations)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    public TenantFlywayMigrator tenantFlywayMigrator(DataSource dataSource,
                                                     @Value("${ciyex.env:local}") String environment) {
        String[] locations = resolveFlywayLocations("db/migration/tenant", environment);
        return new TenantFlywayMigrator(dataSource, locations);
    }

    private String[] resolveFlywayLocations(String basePath, String environment) {
        List<String> locations = new ArrayList<>();
        addIfExists(locations, basePath, "base");
        addIfExists(locations, basePath, "common");

        if (StringUtils.hasText(environment)) {
            expandEnvironmentTokens(environment).forEach(envToken -> addIfExists(locations, basePath, envToken));
        }

        if (locations.isEmpty()) {
            locations.add("classpath:" + basePath);
        }

        return locations.toArray(new String[0]);
    }

    private void addIfExists(List<String> locations, String basePath, String folder) {
        if (!StringUtils.hasText(folder)) {
            return;
        }

        String normalized = folder.trim();
        String resourceLocation = "classpath:" + basePath + "/" + normalized + "/";

        if (resourceLoader.getResource(resourceLocation).exists()) {
            locations.add("classpath:" + basePath + "/" + normalized);
        }
    }

    private Set<String> expandEnvironmentTokens(String environment) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : environment.split(",")) {
            String value = raw.trim();
            if (StringUtils.hasText(value)) {
                tokens.add(value);
            }
        }
        return tokens;
    }
}
