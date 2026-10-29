package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.CropRepository;
import com.index.platform.security.AuthUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final JdbcTemplate jdbc;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    /** Demand vs supply vs sales per crop for one week (Monday start). Powers the admin weekly summary. */
    public Dto.WeeklySummary weeklySummary(LocalDate weekStartParam) {
        LocalDate start = (weekStartParam != null ? weekStartParam : LocalDate.now())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        String sql = """
            SELECT c.id, c.name,
              COALESCE((SELECT SUM(d.quantity) FROM demand_requests d
                        WHERE d.crop_id = c.id AND d.status IN ('OPEN','MATCHED')
                          AND d.needed_by BETWEEN ? AND ?), 0) AS demand_qty,
              COALESCE((SELECT SUM(s.quantity_available) FROM farm_supplies s
                        WHERE s.crop_id = c.id AND s.status = 'AVAILABLE'
                          AND s.available_from <= ? AND (s.available_until IS NULL OR s.available_until >= ?)), 0) AS supply_qty,
              COALESCE((SELECT SUM(sr.quantity) FROM sales_records sr
                        WHERE sr.crop_id = c.id AND sr.sold_on BETWEEN ? AND ?), 0) AS sold_qty
            FROM crops c WHERE c.active
            ORDER BY demand_qty DESC, c.name
            """;
        List<Dto.CropWeekSummary> rows = jdbc.query(sql, (rs, i) -> {
            BigDecimal demand = rs.getBigDecimal("demand_qty");
            BigDecimal supply = rs.getBigDecimal("supply_qty");
            return new Dto.CropWeekSummary(rs.getLong("id"), rs.getString("name"), demand, supply,
                    rs.getBigDecimal("sold_qty"), demand.subtract(supply));
        }, start, end, end, start, start, end);
        return new Dto.WeeklySummary(start, end, rows);
    }

    /** Aggregated open demand per crop. Safe for farmers to see (no shop names). */
    public List<Dto.MarketDemand> marketDemand() {
        String sql = """
            SELECT c.id, c.name, c.unit, SUM(d.quantity) AS total, MIN(d.needed_by) AS earliest, COUNT(*) AS n
            FROM demand_requests d JOIN crops c ON c.id = d.crop_id
            WHERE d.status = 'OPEN' AND d.needed_by >= CURRENT_DATE
            GROUP BY c.id, c.name, c.unit ORDER BY total DESC
            """;
        return jdbc.query(sql, (rs, i) -> new Dto.MarketDemand(rs.getLong("id"), rs.getString("name"),
                rs.getString("unit"), rs.getBigDecimal("total"), rs.getObject("earliest", LocalDate.class), rs.getLong("n")));
    }

    /** Moving-average forecast with a back-test against a naive "same as last week" baseline. */
    public Dto.ForecastResponse forecast(AuthUser u, Long cropId, Long shopIdParam, int weeksParam, int windowParam) {
        int weeks = Math.max(4, Math.min(weeksParam, 52));
        int window = Math.max(2, Math.min(windowParam, 8));
        Long shopId = u.role() == Role.SHOP ? profiles.shop(u).getId() : shopIdParam;
        Crop crop = crops.findById(cropId).orElseThrow(() -> ApiException.notFound("Crop"));

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusWeeks(weeks);
        String sql = "SELECT w.week::date AS week_start, COALESCE(SUM(sr.quantity), 0) AS qty "
                + "FROM generate_series(date_trunc('week', CAST(? AS timestamp)), date_trunc('week', CAST(? AS timestamp)), interval '1 week') AS w(week) "
                + "LEFT JOIN sales_records sr ON sr.crop_id = ? AND sr.sold_on >= w.week::date AND sr.sold_on < w.week::date + 7 "
                + (shopId != null ? "AND sr.shop_id = ? " : "")
                + "GROUP BY w.week ORDER BY w.week";
        List<Object> args = new ArrayList<>(List.of(start, end, cropId));
        if (shopId != null) args.add(shopId);

        List<LocalDate> dates = new ArrayList<>();
        List<Double> actual = new ArrayList<>();
        jdbc.query(sql, rs -> {
            dates.add(rs.getObject("week_start", LocalDate.class));
            actual.add(rs.getBigDecimal("qty").doubleValue());
        }, args.toArray());

        int n = actual.size();
        List<Dto.ForecastPoint> points = new ArrayList<>();
        double maeMa = 0, maeNaive = 0, mapeSum = 0;
        int tested = 0, mapeCount = 0;
        for (int i = 0; i < n; i++) {
            Double predicted = null;
            if (i >= window) {
                predicted = mean(actual.subList(i - window, i));
                double err = Math.abs(actual.get(i) - predicted);
                maeMa += err;
                maeNaive += Math.abs(actual.get(i) - actual.get(i - 1));
                tested++;
                if (actual.get(i) > 0) { mapeSum += err / actual.get(i); mapeCount++; }
            }
            points.add(new Dto.ForecastPoint(dates.get(i), bd(actual.get(i)), predicted == null ? null : bd(predicted)));
        }
        BigDecimal next = n >= window ? bd(mean(actual.subList(n - window, n))) : null;
        BigDecimal maeMaOut = tested > 0 ? bd(maeMa / tested) : null;
        BigDecimal maeNaiveOut = tested > 0 ? bd(maeNaive / tested) : null;
        BigDecimal mapeOut = mapeCount > 0 ? bd(100.0 * mapeSum / mapeCount) : null;
        String better = tested == 0 ? "not enough data yet"
                : (maeMa <= maeNaive ? "moving average" : "naive (last week)");
        return new Dto.ForecastResponse(cropId, crop.getName(), shopId, window, points, next, maeMaOut, maeNaiveOut, mapeOut, better);
    }

    /** Pilot results sheet: what worked, what failed, cost per fulfilled order. */
    public Dto.PilotEconomics pilotEconomics() {
        String sql = """
            SELECT COUNT(*) AS total,
              COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
              COUNT(*) FILTER (WHERE status = 'FAILED') AS failed,
              COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
              COALESCE(SUM(quantity) FILTER (WHERE status = 'COMPLETED'), 0) AS qty,
              COALESCE(SUM(quantity * agreed_price) FILTER (WHERE status = 'COMPLETED'), 0) AS value,
              COALESCE(SUM(transport_cost) FILTER (WHERE status = 'COMPLETED'), 0) AS transport,
              COALESCE(SUM(spoiled_quantity) FILTER (WHERE status IN ('DELIVERED','COMPLETED')), 0) AS spoiled
            FROM orders
            """;
        return jdbc.queryForObject(sql, (rs, i) -> {
            long total = rs.getLong("total"), completed = rs.getLong("completed");
            long failed = rs.getLong("failed"), cancelled = rs.getLong("cancelled");
            BigDecimal transport = rs.getBigDecimal("transport");
            BigDecimal perOrder = completed > 0
                    ? transport.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP) : null;
            long closed = completed + failed;
            BigDecimal rate = closed > 0
                    ? BigDecimal.valueOf(100.0 * completed / closed).setScale(1, RoundingMode.HALF_UP) : null;
            return new Dto.PilotEconomics(total, completed, failed, cancelled, rs.getBigDecimal("qty"),
                    rs.getBigDecimal("value"), transport, rs.getBigDecimal("spoiled"), perOrder, rate);
        });
    }

    /** Headline numbers for the admin dashboard. */
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shops", count("SELECT COUNT(*) FROM users WHERE role = 'SHOP' AND active"));
        m.put("farmers", count("SELECT COUNT(*) FROM users WHERE role = 'FARMER' AND active"));
        m.put("openDemandRequests", count("SELECT COUNT(*) FROM demand_requests WHERE status = 'OPEN'"));
        m.put("availableSupplyListings", count("SELECT COUNT(*) FROM farm_supplies WHERE status = 'AVAILABLE'"));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query("SELECT status, COUNT(*) AS c FROM orders GROUP BY status ORDER BY status",
                rs -> { byStatus.put(rs.getString("status"), rs.getLong("c")); });
        m.put("ordersByStatus", byStatus);
        return m;
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private static double mean(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
