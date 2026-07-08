package com.mockify.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class MockifyBackendApplication {
	public static void main(String[] args) {
		// Set timezone at JVM level
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		// Log message to confirm environment variables were loaded successfully
		log.info("Environment variables natively injected by Spring");
		log.info("JVM default timezone set to: {}", TimeZone.getDefault().getID());
		SpringApplication.run(MockifyBackendApplication.class, args);
	}
}
