package com.equestrian.equis.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

// One PostgreSQL container for the whole test run; each test class gets its own empty database in it.
// Starting a container per class cost about 1.2 s each, while CREATE DATABASE takes milliseconds.
// Testcontainers stops the container when the JVM exits.
//
// Usage in a Spring test:
//   static TestPostgres postgres = TestPostgres.newDatabase();
//   @DynamicPropertySource
//   static void datasource(DynamicPropertyRegistry registry) { postgres.register(registry); }
public final class TestPostgres {
    // Cached Spring contexts keep their connection pools open, and they all share this one server.
    private static final PostgreSQLContainer<?> SERVER = new PostgreSQLContainer<>("postgres:17")
            .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=1000");
    private static final AtomicInteger DATABASES = new AtomicInteger();

    static {
        SERVER.start();
    }

    private final String databaseName;

    private TestPostgres(String databaseName) {
        this.databaseName = databaseName;
    }

    public static TestPostgres newDatabase() {
        String name = "test_" + DATABASES.incrementAndGet();
        try (Connection connection = DriverManager.getConnection(
                SERVER.getJdbcUrl(), SERVER.getUsername(), SERVER.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database " + name, e);
        }
        return new TestPostgres(name);
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + SERVER.getHost() + ":"
                + SERVER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + databaseName;
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
