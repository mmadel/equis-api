package com.equestrian.equis.service.accounts;

import com.equestrian.equis.service.accounts.TokenGenerator.GeneratedToken;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenGeneratorTest {
    private final TokenGenerator tokenGenerator = new TokenGenerator();

    @Test
    void rawTokenIs32UrlSafeBytesWithoutPadding() {
        GeneratedToken token = tokenGenerator.generate();

        assertThat(token.raw()).hasSize(43).matches("^[A-Za-z0-9_-]+$");
        assertThat(Base64.getUrlDecoder().decode(token.raw())).hasSize(32);
    }

    @Test
    void hashIsTheSha256HexOfTheRawToken() {
        GeneratedToken token = tokenGenerator.generate();

        assertThat(token.hash()).matches("^[0-9a-f]{64}$");
        assertThat(token.hash()).isEqualTo(tokenGenerator.hash(token.raw()));
    }

    @Test
    void hashMatchesAKnownSha256Vector() {
        assertThat(tokenGenerator.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void generatesADifferentTokenEachTime() {
        assertThat(tokenGenerator.generate().raw()).isNotEqualTo(tokenGenerator.generate().raw());
    }

    @Test
    void toStringDoesNotExposeTheRawToken() {
        GeneratedToken token = tokenGenerator.generate();

        assertThat(token.toString()).doesNotContain(token.raw());
    }
}
