 package com.qiaben.ciyex;

import com.qiaben.ciyex.config.TelnyxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableConfigurationProperties(TelnyxProperties.class)  // Ensures properties are bound
@EnableMethodSecurity
@EntityScan(basePackages = {"com.qiaben.ciyex.entity", "com.qiaben.ciyex.audit", "com.qiaben.ciyex.auth.scope"})
public class CiyexApplication {

	public static void main(String[] args) {
		SpringApplication.run(CiyexApplication.class, args);
	}

}
