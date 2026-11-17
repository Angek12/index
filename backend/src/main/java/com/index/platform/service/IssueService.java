package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Problem reports on orders (short delivery, spoiled goods, late delivery and so on). */
@Service
@RequiredArgsConstructor
public class IssueService {
    private final OrderIssueRepository issues;
    private final PurchaseOrderRepository orders;
    private final UserRepository users;
    private final ProfileResolver profiles;
    private final NotificationService notifier;
    private final AuditService audit;

    private static final Map<IssueType, String[]> NAMES = Map.of(
            IssueType.SHORT_DELIVERY, new String[]{"short delivery", "ibyagejejwe ni bike"},
            IssueType.SPOILED, new String[]{"spoiled goods", "ibyangiritse"},
            IssueType.WRONG_QUALITY, new String[]{"wrong quality", "ubwiza butandukanye n'ubwumvikanyweho"},
            IssueType.LATE, new String[]{"late delivery", "gutinda kugeza"},
            IssueType.NOT_PAID, new String[]{"payment not received", "amafaranga ataraboneka"},
            IssueType.OTHER, new String[]{"other problem", "ikindi kibazo"});

    private Dto.IssueResponse map(OrderIssue i) {
        PurchaseOrder o = i.getOrder();
        return new Dto.IssueResponse(i.getId(), o.getId(), o.getCrop().getName(), o.getShop().getShopName(), o.getFarmer().getFarmName(),
                i.getReporterRole(), i.getReportedBy().getFullName(), i.getIssueType(), i.getDescription(), i.getQuantityAffected(),
                i.getStatus(), i.getResolution(), i.getResolvedBy() == null ? null : i.getResolvedBy().getFullName(),
                i.getCreatedAt(), i.getResolvedAt());
    }

    private PurchaseOrder order(Long id) { return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order")); }

    private void assertParticipant(AuthUser u, PurchaseOrder o) {
        if (u.role() == Role.ADMIN) return;
        if (profiles.isShopSide(u, o.getShop()) || profiles.isFarmerSide(u, o.getFarmer())) return;
        throw ApiException.forbidden("You are not part of this order");
    }

    @Transactional
    public Dto.IssueResponse create(AuthUser u, Long orderId, Dto.CreateIssueRequest r) {
        PurchaseOrder o = order(orderId);
        if (u.role() == Role.ADMIN) throw ApiException.forbidden("Only the shop or the farmer can report a problem");
        boolean isShop = profiles.isShopSide(u, o.getShop());
        boolean isFarmer = profiles.isFarmerSide(u, o.getFarmer());
        if (!isShop && !isFarmer) throw ApiException.forbidden("You are not part of this order");
        OrderStatus st = o.getStatus();
        if (st == OrderStatus.MATCHED || st == OrderStatus.CANCELLED)
            throw ApiException.badRequest("You can report a problem once the order is confirmed");
        if (r.quantityAffected() != null && r.quantityAffected().compareTo(o.getQuantity()) > 0)
            throw ApiException.badRequest("Quantity affected is more than the order quantity");
        OrderIssue i = new OrderIssue();
        i.setOrder(o);
        i.setReportedBy(users.getReferenceById(u.id()));
        i.setReporterRole(isShop ? Role.SHOP : Role.FARMER);
        i.setIssueType(r.type());
        i.setDescription(r.description().trim());
        i.setQuantityAffected(r.quantityAffected());
        issues.save(i);

        Map<String, String> p = params(o, null);
        p.put("type", NAMES.get(r.type())[0]);
        p.put("type_rw", NAMES.get(r.type())[1]);
        User other = isShop ? o.getFarmer().getUser() : o.getShop().getUser();
        if (!other.getId().equals(u.id())) notifier.send(other, "ISSUE_REPORTED", p);
        notifier.notifyAdmins("ISSUE_REPORTED", p);
        return map(i);
    }

    @Transactional(readOnly = true)
    public List<Dto.IssueResponse> forOrder(AuthUser u, Long orderId) {
        PurchaseOrder o = order(orderId);
        assertParticipant(u, o);
        return issues.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.IssueResponse> adminList(IssueStatus status) {
        List<OrderIssue> list = status == null ? issues.findAllByOrderByCreatedAtDesc() : issues.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::map).toList();
    }

    @Transactional
    public Dto.IssueResponse resolve(AuthUser admin, Long id, Dto.ResolveIssueRequest r) {
        if (r.status() == IssueStatus.OPEN) throw ApiException.badRequest("Choose resolved or dismissed");
        OrderIssue i = issues.findById(id).orElseThrow(() -> ApiException.notFound("Problem report"));
        if (i.getStatus() != IssueStatus.OPEN) throw ApiException.badRequest("This report is already closed");
        i.setStatus(r.status());
        i.setResolution(r.resolution().trim());
        i.setResolvedBy(users.getReferenceById(admin.id()));
        i.setResolvedAt(Instant.now());
        PurchaseOrder o = i.getOrder();
        Map<String, String> p = params(o, r.resolution().trim());
        String key = r.status() == IssueStatus.RESOLVED ? "ISSUE_RESOLVED" : "ISSUE_DISMISSED";
        notifier.send(o.getShop().getUser(), key, p);
        notifier.send(o.getFarmer().getUser(), key, p);
        audit.log(admin.id(), "ISSUE_" + r.status(), "ORDER", o.getId(), r.resolution());
        return map(i);
    }

    private Map<String, String> params(PurchaseOrder o, String note) {
        Map<String, String> m = new HashMap<>();
        m.put("id", String.valueOf(o.getId()));
        m.put("crop", o.getCrop().getName());
        m.put("note", note == null ? "" : note);
        return m;
    }
}
