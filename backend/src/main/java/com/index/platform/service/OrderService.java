package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
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
    private final ProfileResolver profiles;

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

    /** Admin suggests supplies for a demand request: cheapest first, full coverage first. */
    @Transactional(readOnly = true)
    public List<Dto.SupplySuggestion> suggestions(Long demandId) {
        DemandRequest d = demands.findById(demandId).orElseThrow(() -> ApiException.notFound("Demand request"));
        return supplies.findMatchingSupplies(d.getCrop().getId(), SupplyStatus.AVAILABLE, d.getNeededBy()).stream()
                .map(s -> new Dto.SupplySuggestion(Mappers.supply(s), s.getQuantityAvailable().compareTo(d.getQuantity()) >= 0))
                .sorted(Comparator.comparing((Dto.SupplySuggestion x) -> !x.coversFullQuantity())
                        .thenComparing(x -> x.supply().pricePerUnit()))
                .toList();
    }

    @Transactional
    public Dto.OrderResponse create(AuthUser admin, Dto.OrderCreateRequest r) {
        DemandRequest d = demands.findById(r.demandId()).orElseThrow(() -> ApiException.notFound("Demand request"));
        FarmSupply s = supplies.findByIdForUpdate(r.supplyId()).orElseThrow(() -> ApiException.notFound("Supply listing"));
        if (d.getStatus() != DemandStatus.OPEN) throw ApiException.badRequest("This demand request is no longer open");
        if (s.getStatus() != SupplyStatus.AVAILABLE) throw ApiException.badRequest("This supply listing is not available");
        if (!d.getCrop().getId().equals(s.getCrop().getId())) throw ApiException.badRequest("Crop of demand and supply do not match");

        BigDecimal qty = r.quantity() != null ? r.quantity() : d.getQuantity().min(s.getQuantityAvailable());
        if (qty.compareTo(d.getQuantity()) > 0) throw ApiException.badRequest("Quantity is more than the shop requested");
        if (qty.compareTo(s.getQuantityAvailable()) > 0) throw ApiException.badRequest("Quantity is more than the farmer has available");

        PurchaseOrder o = new PurchaseOrder();
        o.setDemandRequest(d); o.setFarmSupply(s); o.setShop(d.getShop()); o.setFarmer(s.getFarmer()); o.setCrop(d.getCrop());
        o.setQuantity(qty);
        o.setAgreedPrice(r.agreedPrice() != null ? r.agreedPrice() : s.getPricePerUnit());
        o.setDeliveryDate(r.deliveryDate() != null ? r.deliveryDate() : d.getNeededBy());
        o.setNotes(r.notes());
        o.setStatus(OrderStatus.MATCHED);

        s.setQuantityAvailable(s.getQuantityAvailable().subtract(qty));
        if (s.getQuantityAvailable().signum() == 0) s.setStatus(SupplyStatus.SOLD_OUT);
        d.setStatus(DemandStatus.MATCHED);

        orders.save(o);
        log(o, null, OrderStatus.MATCHED, admin.id(), "Matched by admin");
        return Mappers.order(o);
    }

    @Transactional(readOnly = true)
    public List<Dto.OrderResponse> list(AuthUser u, OrderStatus status) {
        List<PurchaseOrder> list;
        switch (u.role()) {
            case ADMIN -> list = status == null ? orders.findAllByOrderByCreatedAtDesc() : orders.findByStatusOrderByCreatedAtDesc(status);
            case SHOP -> list = orders.findByShopIdOrderByCreatedAtDesc(profiles.shop(u).getId());
            default -> list = orders.findByFarmerIdOrderByCreatedAtDesc(profiles.farmer(u).getId());
        }
        return list.stream().filter(o -> status == null || o.getStatus() == status).map(Mappers::order).toList();
    }

    @Transactional(readOnly = true)
    public Dto.OrderResponse get(AuthUser u, Long id) {
        PurchaseOrder o = find(id);
        assertParticipant(u, o);
        return Mappers.order(o);
    }

    @Transactional(readOnly = true)
    public List<Dto.HistoryResponse> history(AuthUser u, Long id) {
        PurchaseOrder o = find(id);
        assertParticipant(u, o);
        return history.findByOrderIdOrderByChangedAtAscIdAsc(id).stream().map(Mappers::history).toList();
    }

    @Transactional
    public Dto.OrderResponse updateStatus(AuthUser u, Long id, Dto.OrderStatusRequest r) {
        PurchaseOrder o = find(id);
        boolean admin = u.role() == Role.ADMIN;
        boolean isShop = o.getShop().getUser().getId().equals(u.id());
        boolean isFarmer = o.getFarmer().getUser().getId().equals(u.id());
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
        if (to == OrderStatus.FAILED || to == OrderStatus.CANCELLED) release(o);
        if (to == OrderStatus.COMPLETED) o.getDemandRequest().setStatus(DemandStatus.FULFILLED);
        log(o, from, to, u.id(), r.note());
        return Mappers.order(o);
    }

    /** Give the reserved quantity back to the farmer and reopen the shop's request. */
    private void release(PurchaseOrder o) {
        FarmSupply s = supplies.findByIdForUpdate(o.getFarmSupply().getId()).orElseThrow();
        s.setQuantityAvailable(s.getQuantityAvailable().add(o.getQuantity()));
        if (s.getStatus() == SupplyStatus.SOLD_OUT) s.setStatus(SupplyStatus.AVAILABLE);
        o.getDemandRequest().setStatus(
                o.getDemandRequest().getNeededBy().isBefore(LocalDate.now()) ? DemandStatus.CANCELLED : DemandStatus.OPEN);
    }

    private PurchaseOrder find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private void assertParticipant(AuthUser u, PurchaseOrder o) {
        if (u.role() == Role.ADMIN) return;
        if (o.getShop().getUser().getId().equals(u.id()) || o.getFarmer().getUser().getId().equals(u.id())) return;
        throw ApiException.forbidden("You are not part of this order");
    }

    private void log(PurchaseOrder o, OrderStatus from, OrderStatus to, Long userId, String note) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(o); h.setFromStatus(from); h.setToStatus(to); h.setNote(note);
        h.setChangedBy(userId == null ? null : users.getReferenceById(userId));
        history.save(h);
    }
}
