package com.batch.app;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableAutoConfiguration()
@EnableIntegration
@ComponentScan(basePackages = { "com.batch" })
@EntityScan(basePackages = { "com.batch" })
public class BatchIntegrationApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(BatchIntegrationApplication.class).run(args);
	}

}
