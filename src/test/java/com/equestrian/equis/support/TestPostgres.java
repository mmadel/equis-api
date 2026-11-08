package com.equestrian.equis.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

// A new empty database of its own on the shared server (PostgresContainer), for tests that migrate by hand or need
// Flyway to seed with other placeholders. Every other database test shares the server's own database.
// CREATE DATABASE takes milliseconds.
//
// Usage in a Spring test:
//   static TestPostgres postgres = TestPostgres.newDatabase();
//   @DynamicPropertySource
//   static void datasource(DynamicPropertyRegistry registry) { postgres.register(registry); }
public final class TestPostgres {
    private static final PostgreSQLContainer SERVER = PostgresContainer.POSTGRES;
    private static final AtomicInteger DATABASES = new AtomicInteger();

    private final String databaseName;

    private TestPostgres(String databaseName) {
        this.databaseName = databaseName;
    }

    public static TestPostgres newDatabase() {
        String name = "test_" + DATABASES.incrementAndGet();
        try (Connection connection = PostgresContainer.superuser();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database " + name, e);
        }
        return new TestPostgres(name);
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + SERVER.getHost() + ":"
                + SERVER.getMappedPort(5432) + "/" + databaseName;
    }

    // The same database for code that runs SQL by hand (migration tests): every connection sees every club's rows,
    // as Flyway does (row-level security, V24).
    public String getJdbcUrlSeeingAllClubs() {
        return getJdbcUrl() + "?options=-c%20app.bypass_rls%3Don";
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return SERVER.getUsername();
    }

    public String getPassword() {
        return SERVER.getPassword();
    }

    public void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", this::getJdbcUrl);
        registry.add("spring.datasource.username", this::getUsername);
        registry.add("spring.datasource.password", this::getPassword);
    }
}
