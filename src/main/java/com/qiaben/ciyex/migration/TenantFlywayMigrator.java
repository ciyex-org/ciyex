package com.qiaben.ciyex.migration;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class TenantFlywayMigrator {

    private final DataSource dataSource;
    private final String[] locations;

    public TenantFlywayMigrator(DataSource dataSource, String[] locations) {
        this.dataSource = dataSource;
        this.locations = locations != null ? locations.clone() : new String[0];
    }

    public void migrate(String schemaName, Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId must not be null when running tenant migrations");
        }

    Map<String, String> placeholders = new HashMap<>();
    // Provide a quoted schema placeholder so SQL like SET search_path TO ${schema} is safe
    placeholders.put("schema", com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName));
        placeholders.put("orgId", orgId.toString());

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(locations)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .placeholders(placeholders)
                .load()
                .migrate();
    }
}
