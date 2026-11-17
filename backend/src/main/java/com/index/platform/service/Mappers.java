package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Dto.DemandResponse demand(DemandRequest d, BigDecimal matched) {
        BigDecimal m = matched == null ? BigDecimal.ZERO : matched;
        BigDecimal remaining = d.getQuantity().subtract(m).max(BigDecimal.ZERO);
        return new Dto.DemandResponse(d.getId(), d.getShop().getId(), d.getShop().getShopName(),
                d.getCrop().getId(), d.getCrop().getName(), d.getCrop().getUnit(),
                d.getQuantity(), d.getMaxPrice(), d.getNeededBy(), d.getStatus(), d.getNotes(), d.getCreatedAt(),
                m, remaining);
    }

    public static Dto.SupplyResponse supply(FarmSupply s, Dto.Reputation rep) {
        String photo = s.getPhotoUpdatedAt() == null ? null
                : "/api/photos/" + s.getId() + "?v=" + s.getPhotoUpdatedAt().toEpochMilli();
        return new Dto.SupplyResponse(s.getId(), s.getFarmer().getId(), s.getFarmer().getFarmName(),
                s.getFarmer().getDistrict(), s.getCrop().getId(), s.getCrop().getName(), s.getCrop().getUnit(),
                s.getQuantityAvailable(), s.getPricePerUnit(), s.getAvailableFrom(), s.getAvailableUntil(),
                s.getStatus(), s.getNotes(), s.getCreatedAt(), s.getQualityGrade(), s.getHarvestDate(),
                s.getMinOrderQuantity(), s.getPickupPoint(), photo, s.getFarmer().isCooperative(), rep);
    }

    private static String location(String address, String sector, String district) {
        String s = Stream.of(address, sector, district).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(", "));
        return s.isEmpty() ? null : s;
    }

    /** Contacts are shown only to the other side of an active or finished order, never on failed or cancelled ones. */
    public static Dto.OrderResponse order(PurchaseOrder o, Role viewer, List<OrderRating> ratings, int openIssues) {
        boolean closed = o.getStatus() == OrderStatus.FAILED || o.getStatus() == OrderStatus.CANCELLED;
        boolean showFarmer = !closed && (viewer == Role.SHOP || viewer == Role.ADMIN);
        boolean showShop = !closed && (viewer == Role.FARMER || viewer == Role.ADMIN);
        ShopProfile sh = o.getShop();
        FarmerProfile fa = o.getFarmer();
        Integer shopRating = null, farmerRating = null;
        for (OrderRating r : ratings) {
            if (r.getRaterRole() == Role.SHOP) shopRating = r.getStars();
            else if (r.getRaterRole() == Role.FARMER) farmerRating = r.getStars();
        }
        return new Dto.OrderResponse(o.getId(), o.getDemandRequest().getId(), o.getFarmSupply().getId(),
                sh.getId(), sh.getShopName(), fa.getId(), fa.getFarmName(),
                o.getCrop().getId(), o.getCrop().getName(), o.getCrop().getUnit(), o.getQuantity(),
                o.getAgreedPrice(), o.getQuantity().multiply(o.getAgreedPrice()), o.getDeliveryDate(),
                o.getStatus(), o.getTransportCost(), o.getSpoiledQuantity(), o.getNotes(),
                o.getCreatedAt(), o.getUpdatedAt(),
                o.getPaymentStatus(), o.getPaymentMethod(), o.getPaymentReference(), o.getPaidAt(), o.getReceivedAt(),
                o.getDeliveryMethod(), o.getPickupPoint(), o.getTransportPaidBy(),
                showShop ? sh.getUser().getFullName() : null, showShop ? sh.getUser().getPhone() : null,
                showShop ? location(sh.getAddress(), sh.getSector(), sh.getDistrict()) : null,
                showFarmer ? fa.getUser().getFullName() : null, showFarmer ? fa.getUser().getPhone() : null,
                showFarmer ? location(fa.getAddress(), fa.getSector(), fa.getDistrict()) : null,
                shopRating, farmerRating,
                (!closed && (viewer == Role.SHOP || viewer == Role.ADMIN)) ? o.getDeliveryCode() : null,
                o.getDeliveryCode() != null && (o.getStatus() == OrderStatus.MATCHED || o.getStatus() == OrderStatus.CONFIRMED),
                openIssues, o.getSource());
    }

    public static Dto.HistoryResponse history(OrderStatusHistory h) {
        return new Dto.HistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getChangedBy() == null ? null : h.getChangedBy().getFullName(), h.getNote(), h.getChangedAt());
    }
}
