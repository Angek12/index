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
public class SalesService {
    private final SalesRecordRepository sales;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    private Crop crop(Long id) {
        return crops.findById(id).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
    }

    private SalesRecord build(ShopProfile shop, Dto.SalesRequest r) {
        SalesRecord s = new SalesRecord();
        s.setShop(shop); s.setCrop(crop(r.cropId())); s.setQuantity(r.quantity());
        s.setUnitPrice(r.unitPrice()); s.setSoldOn(r.soldOn());
        return s;
    }

    @Transactional
    public Dto.SalesResponse create(AuthUser u, Dto.SalesRequest r) {
        return Mappers.sales(sales.save(build(profiles.shop(u), r)));
    }

    /** Quick entry: many sales in one go. */
    @Transactional
    public List<Dto.SalesResponse> createBatch(AuthUser u, Dto.BatchSalesRequest r) {
        ShopProfile shop = profiles.shop(u);
        List<Dto.SalesResponse> out = new ArrayList<>();
        for (Dto.SalesRequest item : r.items()) out.add(Mappers.sales(sales.save(build(shop, item))));
        return out;
    }

    @Transactional
    public Dto.SalesResponse update(AuthUser u, Long id, Dto.SalesRequest r) {
        ShopProfile shop = profiles.shop(u);
        SalesRecord s = sales.findById(id).filter(x -> x.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> ApiException.notFound("Sales record"));
        s.setCrop(crop(r.cropId())); s.setQuantity(r.quantity()); s.setUnitPrice(r.unitPrice()); s.setSoldOn(r.soldOn());
        return Mappers.sales(s);
    }

    @Transactional(readOnly = true)
    public List<Dto.SalesResponse> list(AuthUser u, LocalDate from, LocalDate to) {
        ShopProfile shop = profiles.shop(u);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(90);
        return sales.findByShopIdAndSoldOnBetweenOrderBySoldOnDescIdDesc(shop.getId(), start, end)
                .stream().map(Mappers::sales).toList();
    }

    /** Most recent price the shop used for each crop, to pre-fill quick entry. */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> lastPrices(AuthUser u) {
        ShopProfile shop = profiles.shop(u);
        LocalDate end = LocalDate.now();
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        for (SalesRecord s : sales.findByShopIdAndSoldOnBetweenOrderBySoldOnDescIdDesc(shop.getId(), end.minusDays(180), end)) {
            out.putIfAbsent(s.getCrop().getId(), s.getUnitPrice());
        }
        return out;
    }

    @Transactional
    public void delete(AuthUser u, Long id) {
        ShopProfile shop = profiles.shop(u);
        SalesRecord s = sales.findById(id).filter(x -> x.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> ApiException.notFound("Sales record"));
        sales.delete(s);
    }
}
