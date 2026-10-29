package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DemandService {
    private final DemandRequestRepository demands;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    @Transactional
    public Dto.DemandResponse create(AuthUser u, Dto.DemandCreateRequest r) {
        ShopProfile shop = profiles.shop(u);
        Crop crop = crops.findById(r.cropId()).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
        DemandRequest d = new DemandRequest();
        d.setShop(shop); d.setCrop(crop); d.setQuantity(r.quantity()); d.setMaxPrice(r.maxPrice());
        d.setNeededBy(r.neededBy()); d.setNotes(r.notes()); d.setStatus(DemandStatus.OPEN);
        return Mappers.demand(demands.save(d));
    }

    @Transactional(readOnly = true)
    public List<Dto.DemandResponse> mine(AuthUser u) {
        ShopProfile shop = profiles.shop(u);
        return demands.findByShopIdOrderByCreatedAtDesc(shop.getId()).stream().map(Mappers::demand).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.DemandResponse> all(DemandStatus status) {
        List<DemandRequest> list = status == null ? demands.findAllByOrderByCreatedAtDesc()
                : demands.findByStatusOrderByNeededByAsc(status);
        return list.stream().map(Mappers::demand).toList();
    }

    @Transactional
    public Dto.DemandResponse cancel(AuthUser u, Long id) {
        DemandRequest d = demands.findById(id).orElseThrow(() -> ApiException.notFound("Demand request"));
        boolean admin = u.role() == Role.ADMIN;
        if (!admin && !d.getShop().getUser().getId().equals(u.id()))
            throw ApiException.forbidden("This request belongs to another shop");
        if (d.getStatus() != DemandStatus.OPEN)
            throw ApiException.badRequest("Only open requests can be cancelled. Cancel the linked order instead.");
        d.setStatus(DemandStatus.CANCELLED);
        return Mappers.demand(d);
    }
}
