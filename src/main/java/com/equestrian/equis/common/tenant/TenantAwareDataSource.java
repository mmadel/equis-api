package com.equestrian.equis.common.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

// Tells PostgreSQL which club this connection works for, every time it is taken from the pool.
// All three values are set on every checkout, so a pooled connection never keeps the previous user's club.
// The row-level security policies (V24) read them: club -> that club's rows; root -> every row; none -> no rows.
// The role switch to equis_app (V24) is what makes RLS apply at all: it never applies to a superuser.
public class TenantAwareDataSource extends DelegatingDataSource {
    static final String APP_ROLE = "equis_app";
    static final String SET_CLUB = "select set_config('role', '" + APP_ROLE + "', false), "
            + "set_config('app.club_id', ?, false), set_config('app.bypass_rls', ?, false)";

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withClub(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withClub(super.getConnection(username, password));
    }

    private static Connection withClub(Connection connection) throws SQLException {
        UUID club = TenantContext.current();
        try (PreparedStatement statement = connection.prepareStatement(SET_CLUB)) {
            statement.setString(1, club == null ? "" : club.toString());
            statement.setString(2, TenantContext.isRoot() ? "on" : "off");
            statement.execute();
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
