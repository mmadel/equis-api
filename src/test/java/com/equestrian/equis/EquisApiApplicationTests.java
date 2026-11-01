package com.equestrian.equis;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Its own database, so the test doesn't need the dev database or the APP_ADMIN_* variables.
@SpringBootTest(properties = {
		"spring.flyway.placeholders.adminEmail=context-loads-admin@example.com",
		"spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
class EquisApiApplicationTests {

	static TestPostgres postgres = TestPostgres.newDatabase();

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		postgres.register(registry);
	}

	@Test
	void contextLoads() {
	}

}
