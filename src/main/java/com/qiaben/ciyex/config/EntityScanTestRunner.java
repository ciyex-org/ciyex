package com.qiaben.ciyex.config;

import com.qiaben.ciyex.service.TenantSchemaInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntityScanTestRunner implements ApplicationRunner {

    private final TenantSchemaInitializer tenantSchemaInitializer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Wait a bit for application to fully start
        Thread.sleep(2000);
        
        log.info("=== Starting Enhanced Entity Scanning Test ===");
        try {
            // First drop any existing test schema
            tenantSchemaInitializer.dropTenantSchema(99L);
            // Then create fresh schema with detailed logging
            tenantSchemaInitializer.testEnhancedEntityScanning();
        } catch (Exception e) {
            log.error("Entity scanning test failed", e);
        }
        log.info("=== Enhanced Entity Scanning Test Completed ===");
    }
}
