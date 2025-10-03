package com.qiaben.ciyex.service;

import com.qiaben.ciyex.util.SqlIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TenantProvisionService {

    private final DataSource dataSource;

    public TenantProvisionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Provision a tenant schema based on a SQL template file.
     * The template is expected to use the identifier "practice_1" for schema-qualified
     * objects. This method replaces occurrences of that identifier with the target
     * schema name (practice_{orgId}) and executes the resulting SQL against the DB.
     *
     * @param orgId          tenant identifier used in schema name
     * @param templatePath   path on the filesystem to the template SQL file. If null,
     *                       defaults to "schema-dumps/localhost.practice_1.schema.sql"
     */
    public void provisionTenantFromTemplate(String orgId, String templatePath, String sourceSchema) {
        String schemaName = "practice_" + orgId;

        if (templatePath == null || templatePath.isBlank()) {
            // default to the classpath template bundled with the application
            templatePath = "classpath:db/tenant/practice_1.schema.sql";
        }

        // Ensure schema exists using separate connection
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            String createSql = "CREATE SCHEMA IF NOT EXISTS " + SqlIdentifier.quote(schemaName);
            stmt.execute(createSql);
            log.info("Ensured schema exists: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to ensure schema {} exists", schemaName, e);
            throw new TenantProvisionException("Failed to create schema: " + e.getMessage(), e);
        }

        // Read template(s), replace schema token(s), and execute as script(s)
        String source = (sourceSchema == null || sourceSchema.isBlank()) ? "practice_1" : sourceSchema;
        Path p = Path.of(templatePath);

        try {
            if (templatePath.startsWith("classpath:")) {
                // classpath resources: single resource expected
                String cp = templatePath.substring("classpath:".length());
                // Normalize leading slash for ClassLoader
                if (cp.startsWith("/")) {
                    cp = cp.substring(1);
                }
                try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cp)) {
                    if (is == null) {
                        throw new IOException("Classpath resource not found: " + cp);
                    }
                    byte[] bytes = is.readAllBytes();
                    String sql = new String(bytes, StandardCharsets.UTF_8);
                    sql = sql.replace("\"" + source + "\"", "\"" + schemaName + "\"");
                    sql = sql.replaceAll("\\b" + Pattern.quote(source) + "\\b", schemaName);

                    // execute the SQL using a connection where we set search_path to the tenant schema
                    executeSqlOnTenant(schemaName, sql);
                    log.info("Applied classpath template SQL to schema {} from {}", schemaName, templatePath);
                }

            } else if (Files.isDirectory(p)) {
                try (var stream = Files.list(p)) {
                    stream.filter(f -> Files.isRegularFile(f) && f.toString().toLowerCase().endsWith(".sql"))
                          .sorted()
                          .forEach(f -> {
                              try {
                                  processTemplateFile(f, source, schemaName);
                              } catch (Exception e) {
                                  throw new RuntimeException("Failed to process template file: " + f, e);
                              }
                          });
                }
                log.info("Applied all SQL templates in directory {} to schema {}", templatePath, schemaName);

            } else if (Files.isRegularFile(p)) {
                processTemplateFile(p, source, schemaName);
                log.info("Applied template SQL to schema {} from {}", schemaName, templatePath);

            } else {
                throw new IOException("Template path not found: " + templatePath);
            }

        } catch (IOException ioe) {
            log.error("Failed to read template file/path: {}", templatePath, ioe);
            throw new TenantProvisionException("Failed to read template file/path: " + ioe.getMessage(), ioe);
        } catch (Exception ex) {
            log.error("Failed to apply template SQL for {}", schemaName, ex);
            throw new TenantProvisionException("Failed to apply tenant template: " + ex.getMessage(), ex);
        }
    }

    private void processTemplateFile(Path file, String source, String schemaName) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String sql = new String(bytes, StandardCharsets.UTF_8);

        // Replace quoted occurrences and word-boundary occurrences
        sql = sql.replace("\"" + source + "\"", "\"" + schemaName + "\"");
        sql = sql.replaceAll("\\b" + Pattern.quote(source) + "\\b", schemaName);

        // execute the processed SQL in a connection with search_path set to the tenant schema
        executeSqlOnTenant(schemaName, sql);
        log.info("Applied template file {} to schema {}", file, schemaName);
    }

    private void executeSqlOnTenant(String schemaName, String sql) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // set search_path to tenant schema first, then public
            String search = SqlIdentifier.quote(schemaName) + ", public";
            stmt.execute("SET search_path TO " + search);
            // execute script using Spring ScriptUtils to properly handle statements
            EncodedResource resource = new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
            ScriptUtils.executeSqlScript(conn, resource);
        } catch (Exception e) {
            log.error("Failed to execute SQL on tenant {}", schemaName, e);
            throw new TenantProvisionException("Failed to execute SQL on tenant: " + e.getMessage(), e);
        }
    }
}
