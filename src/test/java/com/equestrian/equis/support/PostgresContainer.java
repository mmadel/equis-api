package com.equestrian.equis.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// The one PostgreSQL 17 server of a test JVM, started on first use; Testcontainers stops it when the JVM exits.
// Database test classes (*IT) implement this interface (IntegrationTestBase does): @ServiceConnection points their
// Spring context at the server's database, so they all share it, and FreshDatabase puts the migrated rows back
// before each class. Tests that need a database of their own (hand-run migrations) use TestPostgres.newDatabase().
//
// A static field rather than a @Bean in a @TestConfiguration: Spring stops a container bean when its context closes,
// and the test context cache closes the oldest context once it holds 32, which would stop the server for the rest.
@ExtendWith(FreshDatabase.class)
public interface PostgresContainer {
    @ServiceConnection
    PostgreSQLContainer POSTGRES = start();

    private static PostgreSQLContainer start() {
        // Cached Spring contexts keep their connection pools open, and they all share this one server.
        PostgreSQLContainer server = new PostgreSQLContainer("postgres:17")
                .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=1000");
        server.start();
        return server;
    }

    // A connection as the server's superuser: no row-level security, and DDL is allowed.
    static Connection superuser() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
