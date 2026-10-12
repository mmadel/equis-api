package com.equestrian.equis.service.accounts;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class TokenGenerator {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    // The raw token goes to the client once; only the hash is stored. Never log the raw value.
    public GeneratedToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedToken(raw, hash(raw));
    }

    // SHA-256 as lowercase hex, used both when saving a token and when looking one up.
    public String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record GeneratedToken(String raw, String hash) {
        // Keeps the raw token out of logs if the record is ever printed.
        @Override
        public String toString() {
            return "GeneratedToken[hash=" + hash + "]";
        }
    }
}
