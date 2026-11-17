package com.index.platform.service;

import com.index.platform.dto.Dto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Things that need an admin's attention: stuck orders, overdue deliveries, open problems and so on. */
@Service
@RequiredArgsConstructor
public class AttentionService {
    private final JdbcTemplate jdbc;
    private final NotificationService notifier;

    private record Check(String key, String en, String rw, String sql) {}

    private static final List<Check> CHECKS = List.of(
            new Check("matched_stuck", "orders matched for over 24 hours and not confirmed", "komande zimaze amasaha arenga 24 zihujwe zitaremezwa",
                    "SELECT COUNT(*) FROM orders WHERE status = 'MATCHED' AND created_at < NOW() - INTERVAL '24 hours'"),
            new Check("confirmed_stuck", "orders confirmed for over 48 hours and not delivered", "komande zimaze amasaha arenga 48 zemejwe zitaragezwa",
                    "SELECT COUNT(*) FROM orders WHERE status = 'CONFIRMED' AND updated_at < NOW() - INTERVAL '48 hours'"),
            new Check("overdue", "orders past their delivery date", "komande zarenze itariki yo kugezwa",
                    "SELECT COUNT(*) FROM orders WHERE status IN ('MATCHED','CONFIRMED') AND delivery_date < CURRENT_DATE"),
            new Check("unpaid", "delivered orders not marked as paid for 3 days", "komande zagejejwe zitarishyurwa mu minsi 3",
                    "SELECT COUNT(*) FROM orders WHERE status IN ('DELIVERED','COMPLETED') AND payment_status = 'UNPAID' AND updated_at < NOW() - INTERVAL '3 days'"),
            new Check("payment_unconfirmed", "payments waiting for the farmer to confirm for 3 days", "ubwishyu bumaze iminsi 3 butarahamywa n'umuhinzi",
                    "SELECT COUNT(*) FROM orders WHERE payment_status = 'PAID' AND paid_at < NOW() - INTERVAL '3 days'"),
            new Check("issues", "open problem reports", "ibibazo bitaragenzurwa",
                    "SELECT COUNT(*) FROM order_issues WHERE status = 'OPEN'"),
            new Check("pending_accounts", "accounts waiting for approval for over a day", "konti zimaze umunsi urenga zitegereje kwemezwa",
                    "SELECT COUNT(*) FROM users WHERE approval_status = 'PENDING' AND created_at < NOW() - INTERVAL '24 hours'"),
            new Check("requests_soon", "requests needed within 2 days with no farmer yet", "ibyifuzo bikenewe mu minsi 2 nta muhinzi uragira",
                    "SELECT COUNT(*) FROM demand_requests WHERE status = 'OPEN' AND needed_by <= CURRENT_DATE + 2"),
            new Check("support", "support messages waiting for a reply", "ubutumwa bw'ubufasha butegereje igisubizo",
                    "SELECT COUNT(*) FROM support_tickets WHERE status = 'OPEN'"));

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    @Transactional(readOnly = true)
    public Dto.Attention compute() {
        List<Dto.AttentionItem> items = new ArrayList<>();
        long total = 0;
        for (Check c : CHECKS) {
            long n = count(c.sql());
            items.add(new Dto.AttentionItem(c.key(), c.en(), n));
            total += n;
        }
        return new Dto.Attention(items, total);
    }

    /** One message each morning to every admin, only when something needs attention. */
    @Transactional
    public void sendDigest() {
        StringBuilder en = new StringBuilder();
        StringBuilder rw = new StringBuilder();
        long total = 0;
        for (Check c : CHECKS) {
            long n = count(c.sql());
            if (n == 0) continue;
            total += n;
            if (en.length() > 0) { en.append("; "); rw.append("; "); }
            en.append(n).append(' ').append(c.en());
            rw.append(n).append(' ').append(c.rw());
        }
        if (total == 0) return;
        Map<String, String> p = new LinkedHashMap<>();
        p.put("text", en.toString());
        p.put("text_rw", rw.toString());
        notifier.notifyAdmins("ATTENTION_DIGEST", p);
    }
}
