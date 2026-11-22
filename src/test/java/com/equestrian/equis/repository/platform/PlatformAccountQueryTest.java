package com.equestrian.equis.repository.platform;

import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The SQL itself runs against Postgres in the *IT tests; this checks only how the filters become conditions.
class PlatformAccountQueryTest {
    private static final UUID CLUB = UUID.randomUUID();

    @Test
    void noFilterNoCondition() {
        assertThat(PlatformAccountQuery.where(new AccountFilter(null, null, null))).isEmpty();
    }

    @Test
    void everyFilterIsAndedOn() {
        AccountFilter all = new AccountFilter("Om", AccountType.TRAINER, PlatformAccountStatus.INVITED);

        assertThat(PlatformAccountQuery.where(all))
                .startsWith(" WHERE (lower(name) LIKE :q")
                .contains(" AND type = :type", " AND status = :status");
        MapSqlParameterSource params = PlatformAccountQuery.params(CLUB, all);
        assertThat(params.getValue("clubId")).isEqualTo(CLUB);
        assertThat(params.getValue("q")).isEqualTo("%om%");
        assertThat(params.getValue("type")).isEqualTo("TRAINER");
        assertThat(params.getValue("status")).isEqualTo("INVITED");
    }

    @Test
    void likeWildcardsInTheSearchAreLiteral() {
        assertThat(PlatformAccountQuery.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
    }
}
