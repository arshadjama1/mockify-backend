package com.mockify.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class MockifyBackendApplication {
	public static void main(String[] args) {
		// Load environment variables from .env file at project root
		// Using ignoreIfMissing() so app won't crash if .env is missing
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// Set DB credentials as system properties for Spring to read in application.properties
		System.setProperty("DB_URL", dotenv.get("DB_URL"));
		System.setProperty("DB_USER", dotenv.get("DB_USER"));
		System.setProperty("DB_PASS", dotenv.get("DB_PASS"));

		// Inject JWT credentials from .env
		System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
		System.setProperty("JWT_ACCESS_EXPIRATION", dotenv.get("JWT_ACCESS_EXPIRATION"));
		System.setProperty("JWT_REFRESH_EXPIRATION", dotenv.get("JWT_REFRESH_EXPIRATION"));

		// Set timezone at JVM level
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        // Inject Google's OAuth credentials from .env
		System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
		System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));

		// Log message to confirm environment variables were loaded successfully
        log.info("Environment variables loaded");
        log.info("JVM default timezone set to: {}", TimeZone.getDefault().getID());
		SpringApplication.run(MockifyBackendApplication.class, args);
	}
}
