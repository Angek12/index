package com.index.platform.service;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily jobs. Times are Kigali time. */
@Component
@RequiredArgsConstructor
public class ScheduledJobs {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);
    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private final StandingOrderService standing;
    private final AttentionService attention;

    @Scheduled(cron = "0 0 5 * * *", zone = "Africa/Kigali")
    public void repeatingRequests() {
        try {
            int n = standing.runDue(LocalDate.now(KIGALI));
            log.info("Repeating requests created: {}", n);
        } catch (Exception e) {
            log.error("Repeating requests job failed", e);
        }
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Africa/Kigali")
    public void adminDigest() {
        try {
            attention.sendDigest();
        } catch (Exception e) {
            log.error("Admin digest job failed", e);
        }
    }
}
