package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final ReputationService reputation;

    public Dto.SupplyResponse toResponse(FarmSupply s) {
        return Mappers.supply(s, reputation.farmer(s.getFarmer().getId()));
    }

    @Transactional
    public Dto.SupplyResponse create(AuthUser u, Dto.SupplyCreateRequest r) {
        FarmerProfile farmer = profiles.farmer(u);
        Crop crop = crops.findById(r.cropId()).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
        checkDates(r.availableFrom(), r.availableUntil());
        FarmSupply s = new FarmSupply();
        s.setFarmer(farmer); s.setCrop(crop); s.setQuantityAvailable(r.quantityAvailable());
        s.setPricePerUnit(r.pricePerUnit()); s.setAvailableFrom(r.availableFrom());
        s.setAvailableUntil(r.availableUntil()); s.setQualityGrade(r.qualityGrade()); s.setHarvestDate(r.harvestDate());
        s.setMinOrderQuantity(r.minOrderQuantity()); s.setPickupPoint(r.pickupPoint());
        s.setNotes(r.notes()); s.setStatus(SupplyStatus.AVAILABLE);
        return toResponse(supplies.save(s));
    }

    @Transactional(readOnly = true)
    public List<Dto.SupplyResponse> mine(AuthUser u) {
        FarmerProfile farmer = profiles.farmer(u);
        return supplies.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.SupplyResponse> all(SupplyStatus status) {
        List<FarmSupply> list = status == null ? supplies.findAllByOrderByCreatedAtDesc()
                : supplies.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::toResponse).toList();
    }

    /** Everything shops can order directly: available listings, optionally for one crop or district. */
    @Transactional(readOnly = true)
    public List<Dto.SupplyResponse> marketListings(Long cropId, String district) {
        return supplies.findMarketListings(SupplyStatus.AVAILABLE, LocalDate.now()).stream()
                .filter(s -> cropId == null || s.getCrop().getId().equals(cropId))
                .filter(s -> district == null || district.isBlank() || district.equalsIgnoreCase(s.getFarmer().getDistrict()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Dto.SupplyResponse get(AuthUser u, Long id) { return toResponse(owned(u, id)); }

    @Transactional
    public Dto.SupplyResponse update(AuthUser u, Long id, Dto.SupplyUpdateRequest r) {
        FarmSupply s = owned(u, id);
        if (s.getStatus() == SupplyStatus.WITHDRAWN) throw ApiException.badRequest("Withdrawn listings cannot be edited");
        checkDates(r.availableFrom(), r.availableUntil());
        s.setQuantityAvailable(r.quantityAvailable());
        s.setPricePerUnit(r.pricePerUnit());
        s.setAvailableFrom(r.availableFrom());
        s.setAvailableUntil(r.availableUntil());
        s.setQualityGrade(r.qualityGrade()); s.setHarvestDate(r.harvestDate());
        s.setMinOrderQuantity(r.minOrderQuantity()); s.setPickupPoint(r.pickupPoint());
        s.setNotes(r.notes());
        s.setStatus(r.quantityAvailable().compareTo(BigDecimal.ZERO) > 0 ? SupplyStatus.AVAILABLE : SupplyStatus.SOLD_OUT);
        return toResponse(s);
    }

    @Transactional
    public Dto.SupplyResponse withdraw(AuthUser u, Long id) {
        FarmSupply s = owned(u, id);
        s.setStatus(SupplyStatus.WITHDRAWN);
        return toResponse(s);
    }

    public FarmSupply owned(AuthUser u, Long id) {
        FarmSupply s = supplies.findById(id).orElseThrow(() -> ApiException.notFound("Supply listing"));
        if (u.role() != Role.ADMIN && !profiles.isFarmerSide(u, s.getFarmer()))
            throw ApiException.forbidden("This listing belongs to another farmer");
        return s;
    }

    private void checkDates(LocalDate from, LocalDate until) {
        if (until != null && until.isBefore(from)) throw ApiException.badRequest("availableUntil must not be before availableFrom");
    }
}
