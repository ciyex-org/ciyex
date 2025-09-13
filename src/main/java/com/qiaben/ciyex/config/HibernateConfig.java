package com.qiaben.ciyex.config;

import com.qiaben.ciyex.interceptor.JpaSchemaInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateConfig implements HibernatePropertiesCustomizer {
    
    @Autowired
    private JpaSchemaInterceptor jpaSchemaInterceptor;
    
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.interceptor", jpaSchemaInterceptor);
    }
}
