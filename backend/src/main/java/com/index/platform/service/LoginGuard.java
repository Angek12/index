package com.index.platform.service;

import com.index.platform.exception.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Blocks an account for 15 minutes after 5 wrong passwords. Kept in memory, which is enough for one server. */
@Component
public class LoginGuard {
    public static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void check(String key) {
        Deque<Instant> d = failures.get(key);
        if (d == null) return;
        synchronized (d) {
            prune(d);
            if (d.size() >= MAX_FAILURES) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please try again in 15 minutes.");
            }
        }
    }

    public void failure(String key) {
        Deque<Instant> d = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            prune(d);
            d.addLast(Instant.now());
        }
    }

    public void success(String key) { failures.remove(key); }

    private void prune(Deque<Instant> d) {
        Instant cut = Instant.now().minus(WINDOW);
        while (!d.isEmpty() && d.peekFirst().isBefore(cut)) d.pollFirst();
    }
}
