package com.equestrian.equis.support;

import com.equestrian.equis.entity.assessment.Member;

import java.util.concurrent.atomic.AtomicInteger;

// Every member needs a unique phone and email. For tests that don't care about them, these are made up:
// phones 0177xxxxxxx and emails memberN@mail.eg, unique within the test run.
public final class TestMembers {
    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestMembers() {
    }

    public static Member member(String name) {
        return member(name, null);
    }

    // A null phone gets a made-up one.
    public static Member member(String name, String phone) {
        int n = SEQ.incrementAndGet();
        return Member.register(name, phone != null ? phone : "0177%07d".formatted(n), "member%d@mail.eg".formatted(n));
    }

    public static String nextPhone() {
        return "0177%07d".formatted(SEQ.incrementAndGet());
    }

    public static String nextEmail() {
        return "member%d@mail.eg".formatted(SEQ.incrementAndGet());
    }
}
