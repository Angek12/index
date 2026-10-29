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
public class SupplyService {
    private final FarmSupplyRepository supplies;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    @Transactional
    public Dto.SupplyResponse create(AuthUser u, Dto.SupplyCreateRequest r) {
        FarmerProfile farmer = profiles.farmer(u);
        Crop crop = crops.findById(r.cropId()).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
        checkDates(r.availableFrom(), r.availableUntil());
        FarmSupply s = new FarmSupply();
        s.setFarmer(farmer); s.setCrop(crop); s.setQuantityAvailable(r.quantityAvailable());
        s.setPricePerUnit(r.pricePerUnit()); s.setAvailableFrom(r.availableFrom());
        s.setAvailableUntil(r.availableUntil()); s.setNotes(r.notes()); s.setStatus(SupplyStatus.AVAILABLE);
        return Mappers.supply(supplies.save(s));
    }

    @Transactional(readOnly = true)
    public List<Dto.SupplyResponse> mine(AuthUser u) {
        FarmerProfile farmer = profiles.farmer(u);
        return supplies.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(Mappers::supply).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.SupplyResponse> all(SupplyStatus status) {
        List<FarmSupply> list = status == null ? supplies.findAllByOrderByCreatedAtDesc()
                : supplies.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(Mappers::supply).toList();
    }

    @Transactional
    public Dto.SupplyResponse update(AuthUser u, Long id, Dto.SupplyUpdateRequest r) {
        FarmSupply s = owned(u, id);
        if (s.getStatus() == SupplyStatus.WITHDRAWN) throw ApiException.badRequest("Withdrawn listings cannot be edited");
        checkDates(r.availableFrom(), r.availableUntil());
        s.setQuantityAvailable(r.quantityAvailable());
        s.setPricePerUnit(r.pricePerUnit());
        s.setAvailableFrom(r.availableFrom());
        s.setAvailableUntil(r.availableUntil());
        s.setNotes(r.notes());
        s.setStatus(r.quantityAvailable().compareTo(BigDecimal.ZERO) > 0 ? SupplyStatus.AVAILABLE : SupplyStatus.SOLD_OUT);
        return Mappers.supply(s);
    }

    @Transactional
    public Dto.SupplyResponse withdraw(AuthUser u, Long id) {
        FarmSupply s = owned(u, id);
        s.setStatus(SupplyStatus.WITHDRAWN);
        return Mappers.supply(s);
    }

    private FarmSupply owned(AuthUser u, Long id) {
        FarmSupply s = supplies.findById(id).orElseThrow(() -> ApiException.notFound("Supply listing"));
        if (u.role() != Role.ADMIN && !s.getFarmer().getUser().getId().equals(u.id()))
            throw ApiException.forbidden("This listing belongs to another farmer");
        return s;
    }

    private void checkDates(java.time.LocalDate from, java.time.LocalDate until) {
        if (until != null && until.isBefore(from)) throw ApiException.badRequest("availableUntil must not be before availableFrom");
    }
}
