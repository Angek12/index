package com.index.platform.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Tells admins when the server hits an unexpected error. At most one message every 10 minutes. */
@Service
@RequiredArgsConstructor
public class ErrorAlertService {
    private static final Logger log = LoggerFactory.getLogger(ErrorAlertService.class);
    private static final Duration GAP = Duration.ofMinutes(10);
    private final NotificationService notifier;
    private volatile Instant last = Instant.EPOCH;

    public void report(Exception e) {
        Instant now = Instant.now();
        if (last.plus(GAP).isAfter(now)) return;
        last = now;
        try {
            String msg = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
            if (msg.length() > 200) msg = msg.substring(0, 200);
            notifier.notifyAdmins("SYSTEM_ERROR", Map.of("text", msg));
        } catch (Exception ex) {
            log.warn("Could not send the error alert: {}", ex.getMessage());
        }
    }
}
