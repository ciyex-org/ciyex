package org.ciyex.ehr;

import org.ciyex.ehr.config.TelnyxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableConfigurationProperties(TelnyxProperties.class)
@EnableMethodSecurity
@EnableScheduling
@EnableAsync
public class CiyexApplication {

	public static void main(String[] args) {
		SpringApplication.run(CiyexApplication.class, args);
	}

}
