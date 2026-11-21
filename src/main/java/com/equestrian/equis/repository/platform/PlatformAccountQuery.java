package com.equestrian.equis.repository.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountRow;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// Every account of one club for the platform's Accounts tab: its admins, trainers and riders in one UNION ALL.
// Platform requests run in root mode, where row-level security lets every club through, so each part filters
// club_id itself. Applicants (members never placed) have no rider row and are not listed. The rider's name, phone
// and email are its own columns: copied from the member at placement, and edited on the rider afterwards.
@Repository
public class PlatformAccountQuery {
    private static final String ACCOUNTS = """
            SELECT id, 'ADMIN' AS type, 0 AS type_order, name, email, CAST(NULL AS varchar) AS phone,
                   status AS status, last_login_at AS last_sign_in_at
            FROM admin_account WHERE club_id = :clubId
            UNION ALL
            SELECT id, 'TRAINER', 1, name, email, phone,
                   upper(status), last_active_at
            FROM trainer WHERE club_id = :clubId
            UNION ALL
            SELECT id, 'RIDER', 2, name, email, phone,
                   CASE account_status WHEN 'none' THEN 'NO_ACCOUNT' ELSE upper(account_status) END, last_active_at
            FROM rider WHERE club_id = :clubId""";

    private static final RowMapper<PlatformAccountRow> ROW = (rs, n) -> {
        Timestamp lastSignIn = rs.getTimestamp("last_sign_in_at");
        return new PlatformAccountRow(rs.getObject("id", UUID.class), AccountType.valueOf(rs.getString("type")),
                rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                PlatformAccountStatus.valueOf(rs.getString("status")),
                lastSignIn == null ? null : lastSignIn.toInstant());
    };

    private final NamedParameterJdbcTemplate jdbc;

    public PlatformAccountQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Admins, then trainers, then riders; by name in any case inside each type, then by id so pages never overlap.
    public List<PlatformAccountRow> find(UUID clubId, AccountFilter filter, int page, int size) {
        requireRoot();
        MapSqlParameterSource params = params(clubId, filter)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        String sql = "SELECT * FROM (" + ACCOUNTS + ") a" + where(filter)
                + " ORDER BY type_order, lower(name), id LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params, ROW);
    }

    public long count(UUID clubId, AccountFilter filter) {
        requireRoot();
        String sql = "SELECT count(*) FROM (" + ACCOUNTS + ") a" + where(filter);
        Long n = jdbc.queryForObject(sql, params(clubId, filter), Long.class);
        return n == null ? 0 : n;
    }

    // Only the filters given; Postgres can't type a bare null parameter in "(:q IS NULL OR ...)".
    static String where(AccountFilter filter) {
        List<String> conditions = new ArrayList<>();
        if (filter.q() != null) {
            conditions.add("(lower(name) LIKE :q OR lower(coalesce(email, '')) LIKE :q OR coalesce(phone, '') LIKE :q)");
        }
        if (filter.type() != null) {
            conditions.add("type = :type");
        }
        if (filter.status() != null) {
            conditions.add("status = :status");
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    static MapSqlParameterSource params(UUID clubId, AccountFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource("clubId", clubId);
        if (filter.q() != null) {
            params.addValue("q", "%" + escapeLike(filter.q().toLowerCase(Locale.ROOT)) + "%");
        }
        if (filter.type() != null) {
            params.addValue("type", filter.type().name());
        }
        if (filter.status() != null) {
            params.addValue("status", filter.status().name());
        }
        return params;
    }

    // q is matched literally: % and _ typed by the super admin are not wildcards. Backslash is Postgres's LIKE escape.
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void requireRoot() {
        if (!TenantContext.isRoot()) {
            throw new IllegalStateException("Club accounts are read in root mode");
        }
    }
}
