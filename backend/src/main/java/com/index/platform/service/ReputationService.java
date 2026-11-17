package com.index.platform.service;

import com.index.platform.dto.Dto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reliability (completed vs failed orders) and star ratings. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReputationService {
    private final JdbcTemplate jdbc;

    public Dto.Reputation farmer(Long farmerId) { return build("farmer_id", "SHOP", farmerId); }

    public Dto.Reputation shop(Long shopId) { return build("shop_id", "FARMER", shopId); }

    private Dto.Reputation build(String col, String raterRole, Long id) {
        long[] c = new long[2];
        jdbc.query("SELECT COUNT(*) FILTER (WHERE status = 'COMPLETED') AS c, COUNT(*) FILTER (WHERE status = 'FAILED') AS f "
                + "FROM orders WHERE " + col + " = ?", rs -> {
            c[0] = rs.getLong("c");
            c[1] = rs.getLong("f");
        }, id);
        BigDecimal[] avg = new BigDecimal[1];
        long[] n = new long[1];
        jdbc.query("SELECT AVG(r.stars) AS a, COUNT(*) AS n FROM order_ratings r JOIN orders o ON o.id = r.order_id "
                + "WHERE r.rater_role = ? AND o." + col + " = ?", rs -> {
            avg[0] = rs.getBigDecimal("a");
            n[0] = rs.getLong("n");
        }, raterRole, id);
        BigDecimal reliability = (c[0] + c[1]) > 0
                ? BigDecimal.valueOf(100.0 * c[0] / (c[0] + c[1])).setScale(1, RoundingMode.HALF_UP) : null;
        BigDecimal stars = avg[0] == null ? null : avg[0].setScale(1, RoundingMode.HALF_UP);
        return new Dto.Reputation(c[0], c[1], reliability, stars, n[0]);
    }
}
