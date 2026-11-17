package com.index.platform.service;

import com.index.platform.dto.Dto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Numbers for the admin KPI dashboard. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiService {
    private final JdbcTemplate jdbc;

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private BigDecimal num(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? null : v.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v.setScale(0, RoundingMode.HALF_UP);
    }

    public Dto.Kpis compute(int daysParam) {
        int days = Math.max(7, Math.min(daysParam, 365));
        Timestamp since = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));

        long newRequests = count("SELECT COUNT(*) FROM demand_requests WHERE created_at >= ?", since);
        long created = count("SELECT COUNT(*) FROM orders WHERE created_at >= ?", since);
        long completed = count("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED' AND updated_at >= ?", since);

        BigDecimal avgHours = num("""
            SELECT AVG(EXTRACT(EPOCH FROM (f.first_order - d.created_at)) / 3600.0)
            FROM demand_requests d
            JOIN (SELECT demand_request_id, MIN(created_at) AS first_order FROM orders GROUP BY demand_request_id) f
              ON f.demand_request_id = d.id
            WHERE d.created_at >= ? AND f.first_order >= d.created_at
              AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.demand_request_id = d.id AND o.source = 'MARKETPLACE')
            """, since);

        BigDecimal matchedShare = num("""
            SELECT COUNT(*) FILTER (WHERE status IN ('PARTIAL','MATCHED','FULFILLED')) * 100.0
                   / NULLIF(COUNT(*) FILTER (WHERE status <> 'CANCELLED'), 0)
            FROM demand_requests WHERE created_at >= ?
            """, since);

        BigDecimal onTime = num("""
            SELECT COUNT(*) FILTER (WHERE h.changed_at::date <= o.delivery_date) * 100.0 / NULLIF(COUNT(*), 0)
            FROM orders o JOIN order_status_history h ON h.order_id = o.id AND h.to_status = 'DELIVERED'
            WHERE h.changed_at >= ?
            """, since);

        BigDecimal fulfilment = num("""
            SELECT COUNT(*) FILTER (WHERE status = 'COMPLETED') * 100.0
                   / NULLIF(COUNT(*) FILTER (WHERE status IN ('COMPLETED','FAILED')), 0)
            FROM orders WHERE updated_at >= ?
            """, since);

        BigDecimal spoilage = num("""
            SELECT COALESCE(SUM(spoiled_quantity), 0) * 100.0 / NULLIF(SUM(quantity), 0)
            FROM orders WHERE status = 'COMPLETED' AND updated_at >= ?
            """, since);

        BigDecimal value = money("SELECT COALESCE(SUM(quantity * agreed_price), 0) FROM orders WHERE status = 'COMPLETED' AND updated_at >= ?", since);
        BigDecimal quantity = money("SELECT COALESCE(SUM(quantity), 0) FROM orders WHERE status = 'COMPLETED' AND updated_at >= ?", since);

        BigDecimal paidShare = num("""
            SELECT COALESCE(SUM(quantity * agreed_price) FILTER (WHERE payment_status = 'RECEIVED'), 0) * 100.0
                   / NULLIF(SUM(quantity * agreed_price), 0)
            FROM orders WHERE status = 'COMPLETED' AND updated_at >= ?
            """, since);

        long activeShops = count("""
            SELECT COUNT(DISTINCT shop_id) FROM (
              SELECT shop_id FROM sales_records WHERE created_at >= ?
              UNION SELECT shop_id FROM demand_requests WHERE created_at >= ?
              UNION SELECT shop_id FROM orders WHERE created_at >= ?) x
            """, since, since, since);
        long activeFarmers = count("""
            SELECT COUNT(DISTINCT farmer_id) FROM (
              SELECT farmer_id FROM farm_supplies WHERE created_at >= ?
              UNION SELECT farmer_id FROM orders WHERE created_at >= ?) x
            """, since, since);

        BigDecimal repeatShops = num("""
            SELECT COUNT(*) FILTER (WHERE n >= 2) * 100.0 / NULLIF(COUNT(*), 0)
            FROM (SELECT shop_id, COUNT(*) AS n FROM orders WHERE created_at >= ? AND status <> 'CANCELLED' GROUP BY shop_id) x
            """, since);
        BigDecimal repeatFarmers = num("""
            SELECT COUNT(*) FILTER (WHERE n >= 2) * 100.0 / NULLIF(COUNT(*), 0)
            FROM (SELECT farmer_id, COUNT(*) AS n FROM orders WHERE created_at >= ? AND status <> 'CANCELLED' GROUP BY farmer_id) x
            """, since);

        long openIssues = count("SELECT COUNT(*) FROM order_issues WHERE status = 'OPEN'");
        long newIssues = count("SELECT COUNT(*) FROM order_issues WHERE created_at >= ?", since);
        BigDecimal issueRate = created == 0 ? null
                : BigDecimal.valueOf(100.0 * newIssues / created).setScale(1, RoundingMode.HALF_UP);

        BigDecimal marketShare = num("""
            SELECT COUNT(*) FILTER (WHERE source = 'MARKETPLACE') * 100.0 / NULLIF(COUNT(*), 0)
            FROM orders WHERE created_at >= ?
            """, since);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        List<Dto.WeekKpi> weekly = jdbc.query("""
            SELECT w.week::date AS week_start, COUNT(o.id) AS orders_n,
                   COALESCE(SUM(o.quantity * o.agreed_price) FILTER (WHERE o.status NOT IN ('CANCELLED','FAILED')), 0) AS value
            FROM generate_series(date_trunc('week', CAST(? AS timestamp)), date_trunc('week', CAST(? AS timestamp)), interval '1 week') AS w(week)
            LEFT JOIN orders o ON o.created_at >= w.week AND o.created_at < w.week + interval '1 week'
            GROUP BY w.week ORDER BY w.week
            """, (rs, i) -> new Dto.WeekKpi(rs.getObject("week_start", LocalDate.class), rs.getLong("orders_n"),
                rs.getBigDecimal("value").setScale(0, RoundingMode.HALF_UP)), start, end);

        List<Dto.DistrictKpi> districts = jdbc.query("""
            SELECT COALESCE(NULLIF(x.district, ''), 'Unknown') AS district,
                   SUM(x.shops) AS shops_n, SUM(x.farmers) AS farmers_n, SUM(x.orders_n) AS order_count,
                   SUM(x.val) AS val, SUM(x.demand) AS demand, SUM(x.supply) AS supply
            FROM (
              SELECT district, COUNT(*)::numeric AS shops, 0::numeric AS farmers, 0::numeric AS orders_n, 0::numeric AS val, 0::numeric AS demand, 0::numeric AS supply
                FROM shop_profiles GROUP BY district
              UNION ALL
              SELECT district, 0, COUNT(*), 0, 0, 0, 0 FROM farmer_profiles GROUP BY district
              UNION ALL
              SELECT sp.district, 0, 0, COUNT(o.id), COALESCE(SUM(o.quantity * o.agreed_price), 0), 0, 0
                FROM orders o JOIN shop_profiles sp ON sp.id = o.shop_id
                WHERE o.created_at >= ? AND o.status NOT IN ('CANCELLED','FAILED') GROUP BY sp.district
              UNION ALL
              SELECT sp.district, 0, 0, 0, 0,
                     SUM(d.quantity - COALESCE((SELECT SUM(o.quantity) FROM orders o WHERE o.demand_request_id = d.id AND o.status IN ('MATCHED','CONFIRMED','DELIVERED','COMPLETED')), 0)), 0
                FROM demand_requests d JOIN shop_profiles sp ON sp.id = d.shop_id
                WHERE d.status IN ('OPEN','PARTIAL') GROUP BY sp.district
              UNION ALL
              SELECT fp.district, 0, 0, 0, 0, 0, SUM(s.quantity_available)
                FROM farm_supplies s JOIN farmer_profiles fp ON fp.id = s.farmer_id
                WHERE s.status = 'AVAILABLE' GROUP BY fp.district
            ) x
            GROUP BY 1 ORDER BY order_count DESC, district
            """, (rs, i) -> new Dto.DistrictKpi(rs.getString("district"), rs.getLong("shops_n"), rs.getLong("farmers_n"),
                rs.getLong("order_count"), rs.getBigDecimal("val").setScale(0, RoundingMode.HALF_UP),
                rs.getBigDecimal("demand").setScale(0, RoundingMode.HALF_UP),
                rs.getBigDecimal("supply").setScale(0, RoundingMode.HALF_UP)), since);

        return new Dto.Kpis(days, newRequests, created, completed, avgHours, matchedShare, onTime, fulfilment, spoilage,
                value, quantity, paidShare, activeShops, activeFarmers, repeatShops, repeatFarmers,
                openIssues, issueRate, marketShare, weekly, districts);
    }
}
