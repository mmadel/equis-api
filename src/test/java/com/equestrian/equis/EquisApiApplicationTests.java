package com.equestrian.equis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Its own database, so the test doesn't need the dev database or the APP_ADMIN_* variables.
@SpringBootTest(properties = {
		"spring.flyway.placeholders.adminEmail=context-loads-admin@example.com",
		"spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@Testcontainers
class EquisApiApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Test
	void contextLoads() {
	}

}
