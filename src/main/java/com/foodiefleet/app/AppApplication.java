package com.foodiefleet.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.foodiefleet.app.repository")
@EntityScan(basePackages = "com.foodiefleet.app.model")
@EnableAsync
public class AppApplication {

	public static void main(String[] args) {SpringApplication.run(AppApplication.class, args);
	}

}
