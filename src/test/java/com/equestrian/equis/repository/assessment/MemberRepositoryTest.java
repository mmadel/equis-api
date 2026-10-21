package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.entity.assessment.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E11 · Store the new person's details: the member table's phone and email rules (V16).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class MemberRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager entityManager;

    // Case 1: the context only starts if Flyway ran V16 and Hibernate's validate accepts the entity.
    @Test
    void theMigrationRunsAndTheEntityMatchesTheSchema() {
        assertThat(jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                where table_name = 'member' and column_name = 'email'""", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                where table_name = 'member' and column_name = 'phone'""", String.class)).isEqualTo("NO");
    }

    // Status task, case 1 (V17): member.closed_at exists (nullable) and is stored and cleared.
    @Test
    void closedAtIsStoredAndCleared() {
        assertThat(jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                where table_name = 'member' and column_name = 'closed_at'""", String.class)).isEqualTo("YES");

        Member saved = memberRepository.saveAndFlush(Member.register("Omar", "01011112222", "omar@mail.com"));
        assertThat(saved.getClosedAt()).isNull();

        Instant closedOn = Instant.parse("2026-10-20T09:00:00Z");
        saved.close(closedOn);
        memberRepository.saveAndFlush(saved);
        entityManager.clear();
        assertThat(memberRepository.findById(saved.getId()).orElseThrow().getClosedAt()).isEqualTo(closedOn);

        Member read = memberRepository.findById(saved.getId()).orElseThrow();
        read.reopen(Instant.parse("2026-12-01T09:00:00Z"));
        memberRepository.saveAndFlush(read);
        entityManager.clear();
        assertThat(memberRepository.findById(saved.getId()).orElseThrow().isClosed()).isFalse();
    }

    // Case 3
    @Test
    void registerStoresCleanedDetails() {
        Member saved = memberRepository.saveAndFlush(Member.register("  Sara ", " 01011112222 ", " Sara@Mail.COM "));
        entityManager.clear();

        Member read = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(read.getName()).isEqualTo("Sara");
        assertThat(read.getPhone()).isEqualTo("01011112222");
        assertThat(read.getEmail()).isEqualTo("sara@mail.com");
        assertThat(jdbc.queryForObject("select email from member where id = ?", String.class, saved.getId()))
                .isEqualTo("sara@mail.com");
    }

    // Case 4
    @Test
    void anEmailInAnyCaseIsUnique() {
        memberRepository.saveAndFlush(Member.register("Sara", "01011112222", "sara@mail.com"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.register("Sara A.", "01033334444", "SARA@Mail.com")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        ex -> assertThat(Constraints.nameOf(ex)).contains("uq_member_email"));
    }

    // Case 5
    @Test
    void aPhoneIsUnique() {
        memberRepository.saveAndFlush(Member.register("Sara", "01011112222", "sara@mail.com"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.register("Sara A.", "01011112222", "other@mail.com")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        ex -> assertThat(Constraints.nameOf(ex)).contains("uq_member_phone"));
    }

    // Case 6: the schema itself refuses bad rows, whatever writes them.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL,        sara@mail.com, member.phone",
            "01011112222, NULL,          member.email",
            "01011112222, SARA@x.com,    ck_member_email_lower",
            "01011112222, not-an-email,  ck_member_email_shape"})
    void aNativeInsertWithAMissingOrBadValueIsRefused(String phone, String email, String refusedBy) {
        assertThatThrownBy(() -> jdbc.update("insert into member (name, phone, email) values ('Sara', ?, ?)", phone, email))
                .isInstanceOf(DataIntegrityViolationException.class)
                // Plain JDBC, so there is no Hibernate constraint name to read: the driver's message names it.
                .hasMessageContaining(refusedBy.startsWith("member.")
                        ? "null value in column \"" + refusedBy.substring(7) + "\""
                        : refusedBy);
    }

    // Case 7
    @Test
    void updateDetailsSavesTheCleanedValuesAndMovesUpdatedAt() throws InterruptedException {
        Member member = memberRepository.saveAndFlush(Member.register("Sara", "01011112222", "sara@mail.com"));
        Instant before = jdbc.queryForObject("select updated_at from member where id = ?", Instant.class, member.getId());
        Thread.sleep(5);

        member.updateDetails(" Sara Ali ", " 01233334444 ", " SARA.ALI@Mail.com ");
        memberRepository.saveAndFlush(member);
        entityManager.clear();

        Member read = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(read.getName()).isEqualTo("Sara Ali");
        assertThat(read.getPhone()).isEqualTo("01233334444");
        assertThat(read.getEmail()).isEqualTo("sara.ali@mail.com");
        assertThat(read.getUpdatedAt()).isAfter(before);
    }

    // Case 8
    @Test
    void findsAMemberByPhoneOrEmail() {
        Member sara = memberRepository.saveAndFlush(Member.register("Sara", "01011112222", "sara@mail.com"));

        assertThat(memberRepository.findByPhone("01011112222")).contains(sara);
        assertThat(memberRepository.findByEmail("sara@mail.com")).contains(sara);
        assertThat(memberRepository.existsByEmail("sara@mail.com")).isTrue();
        assertThat(memberRepository.findByPhone("01099999999")).isEmpty();
        assertThat(memberRepository.findByEmail("nobody@mail.com")).isEmpty();
        assertThat(memberRepository.existsByEmail("nobody@mail.com")).isFalse();
    }
}
