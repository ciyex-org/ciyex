package com.qiaben.ciyex.auth.scope;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeDataInitializer {

    private final ScopeSeeder seeder;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        seeder.seed(); // idempotent
    }
}
