package com.equestrian.equis.repository.platform;

import com.equestrian.equis.dto.platform.AccountActivityRow;
import com.equestrian.equis.entity.platform.PlatformAccountAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PlatformAccountActionRepository extends JpaRepository<PlatformAccountAction, UUID> {

    // One club's actions, newest first. The ordering is in the query, so pass an unsorted Pageable.
    @Query(value = """
            select new com.equestrian.equis.dto.platform.AccountActivityRow(
                a.id, a.doneAt, a.accountType, a.accountId, a.accountName, a.accountEmail, a.action, s.email)
            from PlatformAccountAction a
            join SuperAdmin s on s.id = a.doneBy
            where a.clubId = :clubId
            order by a.doneAt desc, a.id desc
            """,
            countQuery = "select count(a) from PlatformAccountAction a where a.clubId = :clubId")
    Page<AccountActivityRow> findActivity(@Param("clubId") UUID clubId, Pageable pageable);
}
