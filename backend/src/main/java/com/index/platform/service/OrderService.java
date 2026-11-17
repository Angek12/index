package com.index.platform.service;

import com.index.platform.config.AppSettings;
import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final PurchaseOrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final DemandRequestRepository demands;
    private final FarmSupplyRepository supplies;
    private final UserRepository users;
    private final OrderRatingRepository ratings;
    private final OrderIssueRepository issues;
    private final ProfileResolver profiles;
    private final NotificationService notifier;
    private final ReputationService reputation;
    private final AppSettings settings;

    private final SecureRandom random = new SecureRandom();
    /** Wrong delivery code attempts per order. Kept in memory. */
    private final Map<Long, Integer> codeFailures = new ConcurrentHashMap<>();
    private static final int MAX_CODE_FAILURES = 5;

    /** Statuses that count as stock already promised to a request. */
    public static final List<OrderStatus> ACTIVE =
            List.of(OrderStatus.MATCHED, OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.MATCHED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.FAILED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.FAILED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.COMPLETED, OrderStatus.FAILED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.FAILED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    private BigDecimal sum(Long demandId, List<OrderStatus> statuses) {
        BigDecimal s = orders.sumQuantity(demandId, statuses);
        return s == null ? BigDecimal.ZERO : s;
    }

    private Dto.OrderResponse map(AuthUser u, PurchaseOrder o) {
        int open = (int) issues.countByOrderIdAndStatus(o.getId(), IssueStatus.OPEN);
        return Mappers.order(o, u.role(), ratings.findByOrderId(o.getId()), open);
    }

    // ---------- matching by an admin ----------
    @Transactional(readOnly = true)
    public List<Dto.SupplySuggestion> suggestions(Long demandId) {
        DemandRequest d = demands.findById(demandId).orElseThrow(() -> ApiException.notFound("Demand request"));
        BigDecimal remaining = d.getQuantity().subtract(sum(d.getId(), ACTIVE)).max(BigDecimal.ZERO);
        return supplies.findMatchingSupplies(d.getCrop().getId(), SupplyStatus.AVAILABLE, d.getNeededBy()).stream()
                .map(s -> new Dto.SupplySuggestion(
                        Mappers.supply(s, reputation.farmer(s.getFarmer().getId())),
                        s.getQuantityAvailable().compareTo(remaining) >= 0))
                .sorted(Comparator.comparing((Dto.SupplySuggestion x) -> !x.coversFullQuantity())
                        .thenComparing(x -> x.supply().pricePerUnit()))
                .toList();
    }

    @Transactional
    public Dto.OrderResponse create(AuthUser admin, Dto.OrderCreateRequest r) {
        DemandRequest d = demands.findById(r.demandId()).orElseThrow(() -> ApiException.notFound("Demand request"));
        FarmSupply s = supplies.findByIdForUpdate(r.supplyId()).orElseThrow(() -> ApiException.notFound("Supply listing"));
        if (d.getStatus() != DemandStatus.OPEN && d.getStatus() != DemandStatus.PARTIAL)
            throw ApiException.badRequest("This demand request is no longer open");
        if (s.getStatus() != SupplyStatus.AVAILABLE) throw ApiException.badRequest("This supply listing is not available");
        if (!d.getCrop().getId().equals(s.getCrop().getId())) throw ApiException.badRequest("Crop of demand and supply do not match");

        BigDecimal remaining = d.getQuantity().subtract(sum(d.getId(), ACTIVE));
        BigDecimal qty = r.quantity() != null ? r.quantity() : remaining.min(s.getQuantityAvailable());
        if (qty.signum() <= 0) throw ApiException.badRequest("Quantity must be more than zero");
        if (qty.compareTo(remaining) > 0) throw ApiException.badRequest("Quantity is more than the shop still needs (" + remaining.stripTrailingZeros().toPlainString() + ")");
        checkSupply(s, qty);

        PurchaseOrder o = buildOrder(d, s, qty,
                r.agreedPrice() != null ? r.agreedPrice() : s.getPricePerUnit(),
                r.deliveryDate() != null ? r.deliveryDate() : d.getNeededBy(),
                r.deliveryMethod(), r.pickupPoint(), r.transportPaidBy(), r.notes(), OrderSource.ADMIN, admin.id());
        return map(admin, o);
    }

    // ---------- direct order by a shop from the marketplace ----------
    @Transactional
    public Dto.OrderResponse marketplaceOrder(AuthUser u, Dto.MarketOrderRequest r) {
        if (!settings.marketplaceEnabled()) throw ApiException.forbidden("The marketplace is switched off");
        ShopProfile shop = profiles.shop(u);
        FarmSupply s = supplies.findByIdForUpdate(r.supplyId()).orElseThrow(() -> ApiException.notFound("Supply listing"));
        if (s.getStatus() != SupplyStatus.AVAILABLE) throw ApiException.badRequest("This listing is no longer available");
        if (r.deliveryDate().isBefore(s.getAvailableFrom()) || (s.getAvailableUntil() != null && r.deliveryDate().isAfter(s.getAvailableUntil())))
            throw ApiException.badRequest("The farmer is not offering this produce on that date");
        checkSupply(s, r.quantity());

        DemandRequest d = new DemandRequest();
        d.setShop(shop); d.setCrop(s.getCrop()); d.setQuantity(r.quantity()); d.setMaxPrice(s.getPricePerUnit());
        d.setNeededBy(r.deliveryDate()); d.setNotes("Marketplace order"); d.setStatus(DemandStatus.OPEN);
        demands.save(d);

        PurchaseOrder o = buildOrder(d, s, r.quantity(), s.getPricePerUnit(), r.deliveryDate(), r.deliveryMethod(),
                s.getPickupPoint(), null, r.notes(), OrderSource.MARKETPLACE, u.id());
        return map(u, o);
    }

    private void checkSupply(FarmSupply s, BigDecimal qty) {
        if (qty.compareTo(s.getQuantityAvailable()) > 0) throw ApiException.badRequest("Quantity is more than the farmer has available");
        if (s.getMinOrderQuantity() != null && qty.compareTo(s.getMinOrderQuantity()) < 0 && qty.compareTo(s.getQuantityAvailable()) < 0)
            throw ApiException.badRequest("This listing has a minimum order of " + s.getMinOrderQuantity().stripTrailingZeros().toPlainString());
    }

    /** Creates the order, reserves the stock, updates the request and tells both sides. */
    private PurchaseOrder buildOrder(DemandRequest d, FarmSupply s, BigDecimal qty, BigDecimal price, LocalDate date,
                                     DeliveryMethod method, String pickup, String paidBy, String notes,
                                     OrderSource source, Long actorId) {
        BigDecimal matchedBefore = sum(d.getId(), ACTIVE);
        PurchaseOrder o = new PurchaseOrder();
        o.setDemandRequest(d); o.setFarmSupply(s); o.setShop(d.getShop()); o.setFarmer(s.getFarmer()); o.setCrop(d.getCrop());
        o.setQuantity(qty);
        o.setAgreedPrice(price);
        o.setDeliveryDate(date);
        o.setDeliveryMethod(method != null ? method : DeliveryMethod.TO_BE_AGREED);
        o.setPickupPoint(pickup != null && !pickup.isBlank() ? pickup : s.getPickupPoint());
        o.setTransportPaidBy(paidBy);
        o.setNotes(notes);
        o.setSource(source);
        o.setStatus(OrderStatus.MATCHED);
        if (settings.requireDeliveryCode()) o.setDeliveryCode(String.format("%04d", random.nextInt(10000)));

        s.setQuantityAvailable(s.getQuantityAvailable().subtract(qty));
        if (s.getQuantityAvailable().signum() == 0) s.setStatus(SupplyStatus.SOLD_OUT);
        d.setStatus(matchedBefore.add(qty).compareTo(d.getQuantity()) >= 0 ? DemandStatus.MATCHED : DemandStatus.PARTIAL);

        orders.save(o);
        log(o, null, OrderStatus.MATCHED, actorId, source == OrderSource.MARKETPLACE ? "Ordered by the shop from the marketplace" : "Matched by admin");
        Map<String, String> p = params(o, null, null);
        notifier.send(o.getShop().getUser(), "ORDER_MATCHED", p);
        notifier.send(o.getFarmer().getUser(), "ORDER_MATCHED", p);
        return o;
    }

    // ---------- reading ----------
    @Transactional(readOnly = true)
    public List<Dto.OrderResponse> list(AuthUser u, OrderStatus status) {
        List<PurchaseOrder> list;
        switch (u.role()) {
            case ADMIN -> list = status == null ? orders.findAllByOrderByCreatedAtDesc() : orders.findByStatusOrderByCreatedAtDesc(status);
            case SHOP -> list = orders.findByShopIdOrderByCreatedAtDesc(profiles.shop(u).getId());
            default -> list = orders.findByFarmerIdOrderByCreatedAtDesc(profiles.farmer(u).getId());
        }
        return list.stream().filter(o -> status == null || o.getStatus() == status).map(o -> map(u, o)).toList();
    }

    @Transactional(readOnly = true)
    public Dto.OrderResponse get(AuthUser u, Long id) {
        PurchaseOrder o = find(id);
        assertParticipant(u, o);
        return map(u, o);
    }

    @Transactional(readOnly = true)
    public List<Dto.HistoryResponse> history(AuthUser u, Long id) {
        PurchaseOrder o = find(id);
        assertParticipant(u, o);
        return history.findByOrderIdOrderByChangedAtAscIdAsc(id).stream().map(Mappers::history).toList();
    }

    // ---------- status ----------
    @Transactional
    public Dto.OrderResponse updateStatus(AuthUser u, Long id, Dto.OrderStatusRequest r) {
        PurchaseOrder o = find(id);
        boolean admin = u.role() == Role.ADMIN;
        boolean isShop = profiles.isShopSide(u, o.getShop());
        boolean isFarmer = profiles.isFarmerSide(u, o.getFarmer());
        if (!admin && !isShop && !isFarmer) throw ApiException.forbidden("You are not part of this order");

        OrderStatus from = o.getStatus();
        OrderStatus to = r.status();
        if (!canTransition(from, to)) throw ApiException.badRequest("Cannot move an order from " + from + " to " + to);

        switch (to) {
            case CONFIRMED, DELIVERED -> {
                if (!admin && !isFarmer) throw ApiException.forbidden("Only the farmer or an admin can set " + to);
            }
            case COMPLETED -> {
                if (!admin && !isShop) throw ApiException.forbidden("Only the shop or an admin can complete an order");
            }
            case FAILED, CANCELLED -> {
                if (r.note() == null || r.note().isBlank()) throw ApiException.badRequest("A reason (note) is required");
            }
            default -> { }
        }
        if (to == OrderStatus.DELIVERED && !admin && o.getDeliveryCode() != null) checkDeliveryCode(o, r.deliveryCode());

        if (r.transportCost() != null) {
            if (!admin && !isFarmer) throw ApiException.forbidden("Only the farmer or an admin can record transport cost");
            o.setTransportCost(r.transportCost());
        }
        if (r.spoiledQuantity() != null) {
            if (!admin && !isShop) throw ApiException.forbidden("Only the shop or an admin can record spoiled quantity");
            if (r.spoiledQuantity().compareTo(o.getQuantity()) > 0) throw ApiException.badRequest("Spoiled quantity exceeds the order quantity");
            o.setSpoiledQuantity(r.spoiledQuantity());
        }

        o.setStatus(to);
        log(o, from, to, u.id(), r.note());
        if (to == OrderStatus.FAILED || to == OrderStatus.CANCELLED) release(o);
        if (to == OrderStatus.COMPLETED) refreshDemand(o.getDemandRequest());

        Map<String, String> p = params(o, r.note(), null);
        switch (to) {
            case CONFIRMED -> {
                tell(o.getShop().getUser(), u, "ORDER_CONFIRMED", p);
                if (o.getDeliveryCode() != null) notifier.send(o.getShop().getUser(), "ORDER_CODE", p);
            }
            case DELIVERED -> tell(o.getShop().getUser(), u, "ORDER_DELIVERED", p);
            case COMPLETED -> tell(o.getFarmer().getUser(), u, "ORDER_COMPLETED", p);
            case FAILED -> { tell(o.getShop().getUser(), u, "ORDER_FAILED", p); tell(o.getFarmer().getUser(), u, "ORDER_FAILED", p); }
            case CANCELLED -> { tell(o.getShop().getUser(), u, "ORDER_CANCELLED", p); tell(o.getFarmer().getUser(), u, "ORDER_CANCELLED", p); }
            default -> { }
        }
        codeFailures.remove(o.getId());
        return map(u, o);
    }

    /** The farmer must enter the code the shop sees on its order. Too many wrong tries need an admin. */
    private void checkDeliveryCode(PurchaseOrder o, String given) {
        int fails = codeFailures.getOrDefault(o.getId(), 0);
        if (fails >= MAX_CODE_FAILURES)
            throw ApiException.forbidden("Too many wrong codes. Ask the Index team to confirm this delivery.");
        if (given == null || !o.getDeliveryCode().equals(given.trim())) {
            codeFailures.put(o.getId(), fails + 1);
            throw ApiException.badRequest("Wrong delivery code. Ask the shop for the code shown on its order.");
        }
    }

    // ---------- payment (recorded only, Index never moves money) ----------
    @Transactional
    public Dto.OrderResponse updatePayment(AuthUser u, Long id, Dto.PaymentRequest r) {
        PurchaseOrder o = find(id);
        boolean admin = u.role() == Role.ADMIN;
        boolean isShop = profiles.isShopSide(u, o.getShop());
        boolean isFarmer = profiles.isFarmerSide(u, o.getFarmer());
        if (!admin && !isShop && !isFarmer) throw ApiException.forbidden("You are not part of this order");

        if ("MARK_PAID".equals(r.action())) {
            if (!admin && !isShop) throw ApiException.forbidden("Only the shop or an admin can mark an order as paid");
            if (o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.COMPLETED)
                throw ApiException.badRequest("An order can be marked as paid once it is delivered");
            if (o.getPaymentStatus() != PaymentStatus.UNPAID) throw ApiException.badRequest("This order is already marked as paid");
            if (r.method() == null) throw ApiException.badRequest("Choose how it was paid");
            o.setPaymentStatus(PaymentStatus.PAID);
            o.setPaymentMethod(r.method());
            o.setPaymentReference(r.reference());
            o.setPaidAt(Instant.now());
            log(o, o.getStatus(), o.getStatus(), u.id(), "Marked as paid (" + r.method() + ")");
            tell(o.getFarmer().getUser(), u, "PAYMENT_PAID", params(o, null, r.method()));
        } else {
            if (!admin && !isFarmer) throw ApiException.forbidden("Only the farmer or an admin can confirm payment received");
            if (o.getPaymentStatus() != PaymentStatus.PAID) throw ApiException.badRequest("The shop has not marked this order as paid yet");
            o.setPaymentStatus(PaymentStatus.RECEIVED);
            o.setReceivedAt(Instant.now());
            log(o, o.getStatus(), o.getStatus(), u.id(), "Payment received");
            tell(o.getShop().getUser(), u, "PAYMENT_RECEIVED", params(o, null, null));
        }
        return map(u, o);
    }

    // ---------- delivery arrangement ----------
    @Transactional
    public Dto.OrderResponse updateDelivery(AuthUser u, Long id, Dto.DeliveryRequest r) {
        PurchaseOrder o = find(id);
        assertParticipant(u, o);
        if (o.getStatus() != OrderStatus.MATCHED && o.getStatus() != OrderStatus.CONFIRMED)
            throw ApiException.badRequest("Delivery arrangements can only change before delivery");
        o.setDeliveryMethod(r.method());
        o.setPickupPoint(r.pickupPoint());
        o.setTransportPaidBy(r.transportPaidBy());
        log(o, o.getStatus(), o.getStatus(), u.id(), "Delivery set to " + r.method());
        return map(u, o);
    }

    // ---------- ratings ----------
    @Transactional
    public Dto.OrderResponse rate(AuthUser u, Long id, Dto.RatingRequest r) {
        PurchaseOrder o = find(id);
        if (u.role() == Role.ADMIN) throw ApiException.forbidden("Only the shop or the farmer can rate an order");
        boolean isShop = profiles.isShopSide(u, o.getShop());
        boolean isFarmer = profiles.isFarmerSide(u, o.getFarmer());
        if (!isShop && !isFarmer) throw ApiException.forbidden("You are not part of this order");
        if (o.getStatus() != OrderStatus.COMPLETED && o.getStatus() != OrderStatus.FAILED)
            throw ApiException.badRequest("You can rate an order once it is completed or failed");
        Role rater = isShop ? Role.SHOP : Role.FARMER;
        if (ratings.existsByOrderIdAndRaterRole(id, rater)) throw ApiException.conflict("You already rated this order");
        OrderRating x = new OrderRating();
        x.setOrder(o); x.setRaterRole(rater); x.setStars(r.stars()); x.setComment(r.comment());
        ratings.save(x);
        return map(u, o);
    }

    // ---------- helpers ----------
    private void release(PurchaseOrder o) {
        FarmSupply s = supplies.findByIdForUpdate(o.getFarmSupply().getId()).orElseThrow();
        s.setQuantityAvailable(s.getQuantityAvailable().add(o.getQuantity()));
        if (s.getStatus() == SupplyStatus.SOLD_OUT) s.setStatus(SupplyStatus.AVAILABLE);
        refreshDemand(o.getDemandRequest());
    }

    /** Recomputes a request's status from the orders linked to it. */
    private void refreshDemand(DemandRequest d) {
        BigDecimal matched = sum(d.getId(), ACTIVE);
        BigDecimal completed = sum(d.getId(), List.of(OrderStatus.COMPLETED));
        if (completed.compareTo(d.getQuantity()) >= 0) d.setStatus(DemandStatus.FULFILLED);
        else if (matched.compareTo(d.getQuantity()) >= 0) d.setStatus(DemandStatus.MATCHED);
        else if (matched.signum() > 0) d.setStatus(DemandStatus.PARTIAL);
        else d.setStatus(d.getNeededBy().isBefore(LocalDate.now()) ? DemandStatus.CANCELLED : DemandStatus.OPEN);
    }

    private PurchaseOrder find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private void assertParticipant(AuthUser u, PurchaseOrder o) {
        if (u.role() == Role.ADMIN) return;
        if (profiles.isShopSide(u, o.getShop()) || profiles.isFarmerSide(u, o.getFarmer())) return;
        throw ApiException.forbidden("You are not part of this order");
    }

    private Map<String, String> params(PurchaseOrder o, String note, String method) {
        Map<String, String> m = new HashMap<>();
        m.put("id", String.valueOf(o.getId()));
        m.put("crop", o.getCrop().getName());
        m.put("qty", o.getQuantity().stripTrailingZeros().toPlainString());
        m.put("unit", o.getCrop().getUnit());
        m.put("shop", o.getShop().getShopName());
        m.put("farmer", o.getFarmer().getFarmName());
        m.put("note", note == null ? "" : note);
        m.put("method", method == null ? "" : method);
        m.put("code", o.getDeliveryCode() == null ? "" : o.getDeliveryCode());
        return m;
    }

    /** Notify one person unless they are the one who just made the change. */
    private void tell(User target, AuthUser actor, String key, Map<String, String> p) {
        if (target.getId().equals(actor.id())) return;
        notifier.send(target, key, p);
    }

    private void log(PurchaseOrder o, OrderStatus from, OrderStatus to, Long userId, String note) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(o); h.setFromStatus(from); h.setToStatus(to); h.setNote(note);
        h.setChangedBy(userId == null ? null : users.getReferenceById(userId));
        history.save(h);
    }
}
