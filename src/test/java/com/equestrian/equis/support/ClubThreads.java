package com.equestrian.equis.support;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Thread pools for the race tests. A new thread has no club, so a service call or SQL on it would see no rows
// (row-level security); these threads run in the default club, like the test thread (DefaultClubExtension).
// Requests over MockMvc still run in their token's club.
public final class ClubThreads {
    private ClubThreads() {}

    public static ExecutorService pool(int threads) {
        return Executors.newFixedThreadPool(threads, task -> new Thread(() -> {
            TenantContext.set(Club.DEFAULT_ID);
            task.run();
        }));
    }

    // For CompletableFuture.runAsync(..., ClubThreads.inDefaultClub()): one new thread per task.
    public static Executor inDefaultClub() {
        return task -> new Thread(() -> {
            TenantContext.set(Club.DEFAULT_ID);
            task.run();
        }).start();
    }
}
