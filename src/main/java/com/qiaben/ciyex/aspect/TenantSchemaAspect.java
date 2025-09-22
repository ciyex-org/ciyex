package com.qiaben.ciyex.aspect;

import com.qiaben.ciyex.service.TenantSchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

//@Slf4j
//@Aspect
//@Component
//@RequiredArgsConstructor
public class TenantSchemaAspect {
    
    //private final TenantSchemaService tenantSchemaService;
    
    //@Before("execution(* com.qiaben.ciyex.repository..*(..))")
    //public void setTenantSchema() {
    //    tenantSchemaService.setTenantSchema();
    //    log.debug("Schema set before repository operation");
    //}
}
