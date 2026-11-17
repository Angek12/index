package com.index.platform.web;

import com.index.platform.security.AuthUser;
import com.index.platform.service.ExportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    private final ExportService export;

    private ResponseEntity<byte[]> csv(String name, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(("\uFEFF" + body).getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/orders.csv")
    public ResponseEntity<byte[]> orders(@AuthenticationPrincipal AuthUser u) { return csv("index-orders.csv", export.orders(u)); }

    @GetMapping("/sales.csv")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<byte[]> sales(@AuthenticationPrincipal AuthUser u) { return csv("index-sales.csv", export.sales(u)); }

    @GetMapping("/weekly-summary.csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> weekly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return csv("index-weekly-summary.csv", export.weeklySummary(weekStart));
    }

    @GetMapping("/pilot.csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> pilot() { return csv("index-pilot-results.csv", export.pilot()); }
}
