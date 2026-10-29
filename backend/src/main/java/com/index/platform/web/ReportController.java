package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;

    @GetMapping("/reports/weekly-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Dto.WeeklySummary weekly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return reports.weeklySummary(weekStart);
    }

    @GetMapping("/reports/pilot-economics")
    @PreAuthorize("hasRole('ADMIN')")
    public Dto.PilotEconomics pilot() { return reports.pilotEconomics(); }

    /** Aggregated open demand per crop, visible to farmers so they know what to grow/list. */
    @GetMapping("/market/demand")
    public List<Dto.MarketDemand> marketDemand() { return reports.marketDemand(); }

    @GetMapping("/forecast/crops/{cropId}")
    @PreAuthorize("hasAnyRole('ADMIN','SHOP')")
    public Dto.ForecastResponse forecast(@AuthenticationPrincipal AuthUser u, @PathVariable Long cropId,
                                         @RequestParam(required = false) Long shopId,
                                         @RequestParam(defaultValue = "12") int weeks,
                                         @RequestParam(defaultValue = "4") int window) {
        return reports.forecast(u, cropId, shopId, weeks, window);
    }
}
