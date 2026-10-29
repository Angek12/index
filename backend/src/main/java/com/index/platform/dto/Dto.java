package com.index.platform.dto;

import com.index.platform.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dto {
    private Dto() {}

    // ---------- Auth & profile ----------
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @NotNull Role role,
            @NotBlank @Size(max = 150) String businessName,
            @Size(max = 80) String district,
            @Size(max = 80) String sector,
            @Size(max = 255) String address) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserInfo(Long id, String email, String fullName, String phone, Role role, boolean active,
                           Long profileId, String businessName, String district, String sector, String address) {}

    public record AuthResponse(String token, UserInfo user) {}

    public record ProfileUpdateRequest(
            @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @Size(max = 150) String businessName,
            @Size(max = 80) String district,
            @Size(max = 80) String sector,
            @Size(max = 255) String address) {}

    // ---------- Crops ----------
    public record CropRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 50) String category,
            @Size(max = 20) String unit,
            Boolean active) {}

    public record CropResponse(Long id, String name, String category, String unit, boolean active) {}

    // ---------- Sales ----------
    public record SalesRequest(
            @NotNull Long cropId,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            @NotNull @PastOrPresent LocalDate soldOn) {}

    public record SalesResponse(Long id, Long shopId, String shopName, Long cropId, String cropName, String unit,
                                BigDecimal quantity, BigDecimal unitPrice, BigDecimal total, LocalDate soldOn) {}

    // ---------- Demand ----------
    public record DemandCreateRequest(
            @NotNull Long cropId,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0.00") BigDecimal maxPrice,
            @NotNull @FutureOrPresent LocalDate neededBy,
            @Size(max = 500) String notes) {}

    public record DemandResponse(Long id, Long shopId, String shopName, Long cropId, String cropName, String unit,
                                 BigDecimal quantity, BigDecimal maxPrice, LocalDate neededBy, DemandStatus status,
                                 String notes, Instant createdAt) {}

    // ---------- Supply ----------
    public record SupplyCreateRequest(
            @NotNull Long cropId,
            @NotNull @DecimalMin("0.01") BigDecimal quantityAvailable,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerUnit,
            @NotNull LocalDate availableFrom,
            LocalDate availableUntil,
            @Size(max = 500) String notes) {}

    public record SupplyUpdateRequest(
            @NotNull @DecimalMin("0.00") BigDecimal quantityAvailable,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerUnit,
            @NotNull LocalDate availableFrom,
            LocalDate availableUntil,
            @Size(max = 500) String notes) {}

    public record SupplyResponse(Long id, Long farmerId, String farmerName, String district, Long cropId,
                                 String cropName, String unit, BigDecimal quantityAvailable, BigDecimal pricePerUnit,
                                 LocalDate availableFrom, LocalDate availableUntil, SupplyStatus status,
                                 String notes, Instant createdAt) {}

    public record SupplySuggestion(SupplyResponse supply, boolean coversFullQuantity) {}

    // ---------- Orders ----------
    public record OrderCreateRequest(
            @NotNull Long demandId,
            @NotNull Long supplyId,
            @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0.00") BigDecimal agreedPrice,
            LocalDate deliveryDate,
            @Size(max = 500) String notes) {}

    public record OrderStatusRequest(
            @NotNull OrderStatus status,
            @Size(max = 500) String note,
            @DecimalMin("0.00") BigDecimal transportCost,
            @DecimalMin("0.00") BigDecimal spoiledQuantity) {}

    public record OrderResponse(Long id, Long demandId, Long supplyId, Long shopId, String shopName, Long farmerId,
                                String farmerName, Long cropId, String cropName, String unit, BigDecimal quantity,
                                BigDecimal agreedPrice, BigDecimal totalValue, LocalDate deliveryDate,
                                OrderStatus status, BigDecimal transportCost, BigDecimal spoiledQuantity,
                                String notes, Instant createdAt, Instant updatedAt) {}

    public record HistoryResponse(Long id, OrderStatus fromStatus, OrderStatus toStatus, String changedBy,
                                  String note, Instant changedAt) {}

    // ---------- Reports ----------
    public record CropWeekSummary(Long cropId, String cropName, BigDecimal demandQty, BigDecimal supplyQty,
                                  BigDecimal soldQty, BigDecimal gap) {}

    public record WeeklySummary(LocalDate weekStart, LocalDate weekEnd, List<CropWeekSummary> crops) {}

    public record MarketDemand(Long cropId, String cropName, String unit, BigDecimal totalQuantity,
                               LocalDate earliestNeededBy, long requestCount) {}

    public record ForecastPoint(LocalDate weekStart, BigDecimal actual, BigDecimal predicted) {}

    public record ForecastResponse(Long cropId, String cropName, Long shopId, int window, List<ForecastPoint> history,
                                   BigDecimal nextWeekForecast, BigDecimal maeMovingAverage, BigDecimal maeNaive,
                                   BigDecimal mapeMovingAverage, String betterModel) {}

    public record PilotEconomics(long totalOrders, long completedOrders, long failedOrders, long cancelledOrders,
                                 BigDecimal completedQuantity, BigDecimal completedValue,
                                 BigDecimal totalTransportCost, BigDecimal spoiledQuantity,
                                 BigDecimal transportCostPerCompletedOrder, BigDecimal fulfilmentRatePercent) {}
}
