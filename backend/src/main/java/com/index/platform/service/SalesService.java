package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesService {
    private final SalesRecordRepository sales;
    private final CropRepository crops;
    private final ProfileResolver profiles;

    @Transactional
    public Dto.SalesResponse create(AuthUser u, Dto.SalesRequest r) {
        ShopProfile shop = profiles.shop(u);
        Crop crop = crops.findById(r.cropId()).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
        SalesRecord s = new SalesRecord();
        s.setShop(shop); s.setCrop(crop); s.setQuantity(r.quantity());
        s.setUnitPrice(r.unitPrice()); s.setSoldOn(r.soldOn());
        return Mappers.sales(sales.save(s));
    }

    @Transactional(readOnly = true)
    public List<Dto.SalesResponse> list(AuthUser u, LocalDate from, LocalDate to) {
        ShopProfile shop = profiles.shop(u);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(90);
        return sales.findByShopIdAndSoldOnBetweenOrderBySoldOnDescIdDesc(shop.getId(), start, end)
                .stream().map(Mappers::sales).toList();
    }

    @Transactional
    public void delete(AuthUser u, Long id) {
        ShopProfile shop = profiles.shop(u);
        SalesRecord s = sales.findById(id).filter(x -> x.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> ApiException.notFound("Sales record"));
        sales.delete(s);
    }
}
