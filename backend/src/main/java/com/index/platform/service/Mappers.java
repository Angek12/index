package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import java.math.BigDecimal;

/** Entity -> DTO mapping. Call only inside a transaction (lazy associations). */
public final class Mappers {
    private Mappers() {}

    public static Dto.CropResponse crop(Crop c) {
        return new Dto.CropResponse(c.getId(), c.getName(), c.getCategory(), c.getUnit(), c.isActive());
    }

    public static Dto.SalesResponse sales(SalesRecord s) {
        BigDecimal total = s.getQuantity().multiply(s.getUnitPrice());
        return new Dto.SalesResponse(s.getId(), s.getShop().getId(), s.getShop().getShopName(),
                s.getCrop().getId(), s.getCrop().getName(), s.getCrop().getUnit(),
                s.getQuantity(), s.getUnitPrice(), total, s.getSoldOn());
    }

    public static Dto.DemandResponse demand(DemandRequest d) {
        return new Dto.DemandResponse(d.getId(), d.getShop().getId(), d.getShop().getShopName(),
                d.getCrop().getId(), d.getCrop().getName(), d.getCrop().getUnit(),
                d.getQuantity(), d.getMaxPrice(), d.getNeededBy(), d.getStatus(), d.getNotes(), d.getCreatedAt());
    }

    public static Dto.SupplyResponse supply(FarmSupply s) {
        return new Dto.SupplyResponse(s.getId(), s.getFarmer().getId(), s.getFarmer().getFarmName(),
                s.getFarmer().getDistrict(), s.getCrop().getId(), s.getCrop().getName(), s.getCrop().getUnit(),
                s.getQuantityAvailable(), s.getPricePerUnit(), s.getAvailableFrom(), s.getAvailableUntil(),
                s.getStatus(), s.getNotes(), s.getCreatedAt());
    }

    public static Dto.OrderResponse order(PurchaseOrder o) {
        return new Dto.OrderResponse(o.getId(), o.getDemandRequest().getId(), o.getFarmSupply().getId(),
                o.getShop().getId(), o.getShop().getShopName(), o.getFarmer().getId(), o.getFarmer().getFarmName(),
                o.getCrop().getId(), o.getCrop().getName(), o.getCrop().getUnit(), o.getQuantity(),
                o.getAgreedPrice(), o.getQuantity().multiply(o.getAgreedPrice()), o.getDeliveryDate(),
                o.getStatus(), o.getTransportCost(), o.getSpoiledQuantity(), o.getNotes(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    public static Dto.HistoryResponse history(OrderStatusHistory h) {
        return new Dto.HistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getChangedBy() == null ? null : h.getChangedBy().getFullName(), h.getNote(), h.getChangedAt());
    }
}
