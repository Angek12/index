package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DemandService {
    private final DemandRequestRepository demands;
    private final PurchaseOrderRepository orders;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    private Dto.DemandResponse map(DemandRequest d) {
        BigDecimal matched = orders.sumQuantity(d.getId(), OrderService.ACTIVE);
        return Mappers.demand(d, matched);
    }

    private Crop crop(Long id) {
        return crops.findById(id).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
    }

    @Transactional
    public Dto.DemandResponse create(AuthUser u, Dto.DemandCreateRequest r) {
        DemandRequest d = new DemandRequest();
        d.setShop(profiles.shop(u)); d.setCrop(crop(r.cropId())); d.setQuantity(r.quantity()); d.setMaxPrice(r.maxPrice());
        d.setNeededBy(r.neededBy()); d.setNotes(r.notes()); d.setStatus(DemandStatus.OPEN);
        return map(demands.save(d));
    }

    private DemandRequest ownOpen(AuthUser u, Long id, String what) {
        DemandRequest d = demands.findById(id).orElseThrow(() -> ApiException.notFound("Demand request"));
        boolean admin = u.role() == Role.ADMIN;
        if (!admin && !profiles.isShopSide(u, d.getShop())) throw ApiException.forbidden("This request belongs to another shop");
        if (d.getStatus() != DemandStatus.OPEN)
            throw ApiException.badRequest("Only open requests can be " + what + ". Cancel the linked orders first.");
        return d;
    }

    @Transactional
    public Dto.DemandResponse update(AuthUser u, Long id, Dto.DemandCreateRequest r) {
        DemandRequest d = ownOpen(u, id, "edited");
        d.setCrop(crop(r.cropId())); d.setQuantity(r.quantity()); d.setMaxPrice(r.maxPrice());
        d.setNeededBy(r.neededBy()); d.setNotes(r.notes());
        return map(d);
    }

    /** One tap to ask for the same thing again on a new date. */
    @Transactional
    public Dto.DemandResponse repeat(AuthUser u, Long id, Dto.RepeatRequest r) {
        ShopProfile shop = profiles.shop(u);
        DemandRequest old = demands.findById(id).filter(x -> x.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> ApiException.notFound("Demand request"));
        DemandRequest d = new DemandRequest();
        d.setShop(shop); d.setCrop(crop(old.getCrop().getId())); d.setQuantity(old.getQuantity());
        d.setMaxPrice(old.getMaxPrice()); d.setNotes(old.getNotes()); d.setNeededBy(r.neededBy());
        d.setStatus(DemandStatus.OPEN);
        return map(demands.save(d));
    }

    @Transactional(readOnly = true)
    public List<Dto.DemandResponse> mine(AuthUser u) {
        return demands.findByShopIdOrderByCreatedAtDesc(profiles.shop(u).getId()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.DemandResponse> all(DemandStatus status, boolean needsMatch) {
        List<DemandRequest> list;
        if (needsMatch) list = demands.findByStatusInOrderByNeededByAsc(List.of(DemandStatus.OPEN, DemandStatus.PARTIAL));
        else if (status != null) list = demands.findByStatusOrderByNeededByAsc(status);
        else list = demands.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::map).toList();
    }

    @Transactional
    public Dto.DemandResponse cancel(AuthUser u, Long id) {
        DemandRequest d = ownOpen(u, id, "cancelled");
        d.setStatus(DemandStatus.CANCELLED);
        return map(d);
    }
}
