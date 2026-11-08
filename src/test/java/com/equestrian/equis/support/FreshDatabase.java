package com.equestrian.equis.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Gives every database test class the database as Flyway left it, as when each class had a database of its own.
// All *IT classes of a JVM share one database (PostgresContainer) and, where their set-up is the same, one Spring
// context. The first time, the context's own Flyway set-up migrates a new empty database (TestPostgres) and every
// table's rows are copied out of it (COPY ... TO STDOUT, kept in memory, so no extra table shows up in the schema).
// It is a database of its own because the shared one has more than Flyway's rows once the full app has started (the
// sessions SessionGenerationJob creates on start-up). Before each class, every table of the shared database is
// truncated and the rows copied back in, with the sequences where Flyway left them.
// Runs as the superuser, so row-level security does not apply, and session_replication_role = replica skips the
// foreign-key checks while the tables refill in any order. Registered by PostgresContainer for every class that
// implements it.
public class FreshDatabase implements BeforeAllCallback {
    private static Map<String, byte[]> rows;
    private static List<String> sequences;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Starting the context first, so its own start-up writes come before the clean-up.
        ApplicationContext spring = SpringExtension.getApplicationContext(context);
        restore(spring.getBean(Flyway.class));
        // Beans that remember rows of the last class.
        spring.getBeanProvider(TwoClubFixture.class).ifAvailable(TwoClubFixture::forget);
        spring.getBeanProvider(RecordingNotificationSender.class).ifAvailable(RecordingNotificationSender::clear);
    }

    private static synchronized void restore(Flyway flyway) throws SQLException, IOException {
        if (rows == null) {
            snapshot(flyway);
        }
        try (Connection connection = PostgresContainer.superuser();
             Statement statement = connection.createStatement()) {
            CopyManager copy = connection.unwrap(PGConnection.class).getCopyAPI();
            statement.execute("SET session_replication_role = replica");
            statement.execute("TRUNCATE " + String.join(", ", tables(connection)) + " CASCADE");
            for (Map.Entry<String, byte[]> table : rows.entrySet()) {
                copy.copyIn("COPY " + table.getKey() + " FROM STDIN (FORMAT binary)",
                        new ByteArrayInputStream(table.getValue()));
            }
            for (String setval : sequences) {
                statement.execute(setval);
            }
            statement.execute("SET session_replication_role = origin");
        }
    }

    private static void snapshot(Flyway flyway) throws SQLException, IOException {
        TestPostgres seed = TestPostgres.newDatabase();
        Flyway.configure().configuration(flyway.getConfiguration())
                .dataSource(seed.getJdbcUrl(), seed.getUsername(), seed.getPassword())
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(
                seed.getJdbcUrl(), seed.getUsername(), seed.getPassword())) {
            CopyManager copy = connection.unwrap(PGConnection.class).getCopyAPI();
            Map<String, byte[]> copied = new LinkedHashMap<>();
            for (String table : tables(connection)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                copy.copyOut("COPY " + table + " TO STDOUT (FORMAT binary)", out);
                copied.put(table, out.toByteArray());
            }
            sequences = sequenceValues(connection);
            rows = copied;
        }
    }

    // Every table of the app's schema but Flyway's own history.
    private static List<String> tables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT quote_ident(tablename) FROM pg_tables
                     WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                     ORDER BY tablename""")) {
            while (result.next()) {
                tables.add("public." + result.getString(1));
            }
        }
        return tables;
    }

    // For each sequence, the statement that puts it back where it is now.
    private static List<String> sequenceValues(Connection connection) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT format('SELECT setval(%L, %s, %s)', 'public.' || quote_ident(sequencename),
                                   coalesce(last_value, start_value), last_value IS NOT NULL)
                     FROM pg_sequences WHERE schemaname = 'public'""")) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;
    }
}
