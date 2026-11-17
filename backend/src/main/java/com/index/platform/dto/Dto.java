package com.index.platform.dto;

import com.index.platform.domain.*;
import jakarta.validation.Valid;
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
            @Size(max = 255) String address,
            @Size(max = 5) String language,
            Boolean cooperative,
            Boolean acceptTerms) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserInfo(Long id, String email, String fullName, String phone, Role role, boolean active,
                           Long profileId, String businessName, String district, String sector, String address,
                           String language, boolean notifySms, boolean notifyWhatsapp, ApprovalStatus approvalStatus,
                           boolean cooperative, String teamRole, boolean phoneVerified) {}

    /** token is null when the account is waiting for admin approval. */
    public record AuthResponse(String token, UserInfo user, String message, boolean phoneVerificationRequired) {}

    public record ProfileUpdateRequest(
            @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @Size(max = 150) String businessName,
            @Size(max = 80) String district,
            @Size(max = 80) String sector,
            @Size(max = 255) String address,
            @Size(max = 5) String language,
            Boolean notifySms,
            Boolean notifyWhatsapp) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank @Email String email, @NotBlank String code,
                                       @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record ResetCodeResponse(String code, Instant expiresAt) {}

    // ---------- Team ----------
    public record TeamMember(Long userId, String fullName, String email, String phone, String teamRole, boolean active) {}
    public record AddMemberRequest(@NotBlank @Size(max = 120) String fullName,
                                   @NotBlank @Email @Size(max = 150) String email,
                                   @Size(max = 30) String phone,
                                   @NotBlank @Size(min = 8, max = 72) String password) {}

    // ---------- Notifications ----------
    public record NotificationResponse(Long id, String message, String link, boolean read, Instant createdAt) {}

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

    public record BatchSalesRequest(@NotEmpty @Size(max = 50) List<@Valid SalesRequest> items) {}

    public record SalesResponse(Long id, Long shopId, String shopName, Long cropId, String cropName, String unit,
                                BigDecimal quantity, BigDecimal unitPrice, BigDecimal total, LocalDate soldOn) {}

    // ---------- Demand ----------
    public record DemandCreateRequest(
            @NotNull Long cropId,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0.00") BigDecimal maxPrice,
            @NotNull @FutureOrPresent LocalDate neededBy,
            @Size(max = 500) String notes) {}

    public record RepeatRequest(@NotNull @FutureOrPresent LocalDate neededBy) {}

    public record DemandResponse(Long id, Long shopId, String shopName, Long cropId, String cropName, String unit,
                                 BigDecimal quantity, BigDecimal maxPrice, LocalDate neededBy, DemandStatus status,
                                 String notes, Instant createdAt, BigDecimal matchedQuantity, BigDecimal remainingQuantity) {}

    // ---------- Supply ----------
    public record SupplyCreateRequest(
            @NotNull Long cropId,
            @NotNull @DecimalMin("0.01") BigDecimal quantityAvailable,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerUnit,
            @NotNull LocalDate availableFrom,
            LocalDate availableUntil,
            @Pattern(regexp = "A|B|C", message = "must be A, B or C") String qualityGrade,
            LocalDate harvestDate,
            @DecimalMin("0.00") BigDecimal minOrderQuantity,
            @Size(max = 200) String pickupPoint,
            @Size(max = 500) String notes) {}

    public record SupplyUpdateRequest(
            @NotNull @DecimalMin("0.00") BigDecimal quantityAvailable,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerUnit,
            @NotNull LocalDate availableFrom,
            LocalDate availableUntil,
            @Pattern(regexp = "A|B|C", message = "must be A, B or C") String qualityGrade,
            LocalDate harvestDate,
            @DecimalMin("0.00") BigDecimal minOrderQuantity,
            @Size(max = 200) String pickupPoint,
            @Size(max = 500) String notes) {}

    public record Reputation(long completed, long failed, BigDecimal reliabilityPercent,
                             BigDecimal avgStars, long ratingCount) {}

    public record SupplyResponse(Long id, Long farmerId, String farmerName, String district, Long cropId,
                                 String cropName, String unit, BigDecimal quantityAvailable, BigDecimal pricePerUnit,
                                 LocalDate availableFrom, LocalDate availableUntil, SupplyStatus status,
                                 String notes, Instant createdAt, String qualityGrade, LocalDate harvestDate,
                                 BigDecimal minOrderQuantity, String pickupPoint, String photoUrl,
                                 boolean cooperative, Reputation reputation) {}

    public record SupplySuggestion(SupplyResponse supply, boolean coversFullQuantity) {}

    // ---------- Orders ----------
    public record OrderCreateRequest(
            @NotNull Long demandId,
            @NotNull Long supplyId,
            @DecimalMin("0.01") BigDecimal quantity,
            @DecimalMin("0.00") BigDecimal agreedPrice,
            LocalDate deliveryDate,
            DeliveryMethod deliveryMethod,
            @Size(max = 200) String pickupPoint,
            @Pattern(regexp = "FARMER|SHOP|SHARED", message = "must be FARMER, SHOP or SHARED") String transportPaidBy,
            @Size(max = 500) String notes) {}

    public record OrderStatusRequest(
            @NotNull OrderStatus status,
            @Size(max = 500) String note,
            @DecimalMin("0.00") BigDecimal transportCost,
            @DecimalMin("0.00") BigDecimal spoiledQuantity,
            @Size(max = 6) String deliveryCode) {}

    public record PaymentRequest(
            @NotBlank @Pattern(regexp = "MARK_PAID|CONFIRM_RECEIVED", message = "must be MARK_PAID or CONFIRM_RECEIVED") String action,
            @Pattern(regexp = "CASH|MOBILE_MONEY|BANK_TRANSFER|OTHER", message = "must be CASH, MOBILE_MONEY, BANK_TRANSFER or OTHER") String method,
            @Size(max = 80) String reference) {}

    public record DeliveryRequest(
            @NotNull DeliveryMethod method,
            @Size(max = 200) String pickupPoint,
            @Pattern(regexp = "FARMER|SHOP|SHARED", message = "must be FARMER, SHOP or SHARED") String transportPaidBy) {}

    public record RatingRequest(@Min(1) @Max(5) int stars, @Size(max = 300) String comment) {}

    /** shopRating = stars the shop gave the farmer; farmerRating = stars the farmer gave the shop. */
    public record OrderResponse(Long id, Long demandId, Long supplyId, Long shopId, String shopName, Long farmerId,
                                String farmerName, Long cropId, String cropName, String unit, BigDecimal quantity,
                                BigDecimal agreedPrice, BigDecimal totalValue, LocalDate deliveryDate,
                                OrderStatus status, BigDecimal transportCost, BigDecimal spoiledQuantity,
                                String notes, Instant createdAt, Instant updatedAt,
                                PaymentStatus paymentStatus, String paymentMethod, String paymentReference,
                                Instant paidAt, Instant receivedAt,
                                DeliveryMethod deliveryMethod, String pickupPoint, String transportPaidBy,
                                String shopContactName, String shopPhone, String shopLocation,
                                String farmerContactName, String farmerPhone, String farmerLocation,
                                Integer shopRating, Integer farmerRating,
                                String deliveryCode, boolean deliveryCodeRequired, int openIssues, OrderSource source) {}

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

    public record PricePoint(LocalDate weekStart, BigDecimal retailPrice, BigDecimal agreedPrice) {}

    public record PriceTrend(Long cropId, String cropName, String unit, List<PricePoint> points,
                             BigDecimal latestRetail, BigDecimal latestAgreed) {}

    public record PilotEconomics(long totalOrders, long completedOrders, long failedOrders, long cancelledOrders,
                                 BigDecimal completedQuantity, BigDecimal completedValue,
                                 BigDecimal totalTransportCost, BigDecimal spoiledQuantity,
                                 BigDecimal transportCostPerCompletedOrder, BigDecimal fulfilmentRatePercent,
                                 BigDecimal paymentsReceivedValue, BigDecimal unpaidCompletedValue) {}

    // ---------- Phone verification, admins ----------
    public record VerifyPhoneRequest(@NotBlank @Email String email, @NotBlank String code) {}
    public record ResendCodeRequest(@NotBlank @Email String email) {}
    public record CreateAdminRequest(@NotBlank @Size(max = 120) String fullName,
                                     @NotBlank @Email @Size(max = 150) String email,
                                     @Size(max = 30) String phone,
                                     @NotBlank @Size(min = 8, max = 72) String password) {}

    // ---------- Problem reports ----------
    public record CreateIssueRequest(@NotNull IssueType type, @NotBlank @Size(max = 500) String description,
                                     @DecimalMin("0.00") BigDecimal quantityAffected) {}
    public record ResolveIssueRequest(@NotNull IssueStatus status, @NotBlank @Size(max = 500) String resolution) {}
    public record IssueResponse(Long id, Long orderId, String cropName, String shopName, String farmerName,
                                Role reporterRole, String reporterName, IssueType type, String description,
                                BigDecimal quantityAffected, IssueStatus status, String resolution,
                                String resolvedBy, Instant createdAt, Instant resolvedAt) {}

    // ---------- Admin activity ----------
    public record AuditEntry(Long id, String actor, String action, String targetType, Long targetId,
                             String details, Instant createdAt) {}

    // ---------- Marketplace and standing orders ----------
    public record MarketOrderRequest(@NotNull Long supplyId,
                                     @NotNull @DecimalMin("0.01") BigDecimal quantity,
                                     @NotNull @FutureOrPresent LocalDate deliveryDate,
                                     DeliveryMethod deliveryMethod,
                                     @Size(max = 500) String notes) {}

    public record StandingOrderRequest(@NotNull Long cropId,
                                       @NotNull @DecimalMin("0.01") BigDecimal quantity,
                                       @DecimalMin("0.00") BigDecimal maxPrice,
                                       @NotNull Frequency frequency,
                                       @NotNull @FutureOrPresent LocalDate firstNeededDate,
                                       @Min(0) @Max(14) Integer leadDays,
                                       @Size(max = 500) String notes) {}

    public record StandingOrderResponse(Long id, Long cropId, String cropName, String unit, BigDecimal quantity,
                                        BigDecimal maxPrice, Frequency frequency, LocalDate nextNeededDate,
                                        int leadDays, boolean active, String notes, LocalDate lastRunOn) {}

    // ---------- Support inbox ----------
    public record TicketCreateRequest(@NotBlank @Size(max = 120) String subject, @NotNull TicketCategory category,
                                      @NotBlank @Size(max = 1000) String message) {}
    public record TicketMessageRequest(@NotBlank @Size(max = 1000) String body) {}
    public record TicketSummary(Long id, String subject, TicketCategory category, TicketStatus status,
                                String userName, String userRole, Instant createdAt, Instant updatedAt) {}
    public record TicketMessageResponse(Long id, String senderName, Role senderRole, String body, Instant createdAt) {}
    public record TicketDetail(TicketSummary ticket, List<TicketMessageResponse> messages) {}

    // ---------- Attention and KPIs ----------
    public record AttentionItem(String key, String label, long count) {}
    public record Attention(List<AttentionItem> items, long total) {}

    public record WeekKpi(LocalDate weekStart, long orders, BigDecimal value) {}
    public record DistrictKpi(String district, long shops, long farmers, long orders, BigDecimal value,
                              BigDecimal openDemand, BigDecimal availableSupply) {}
    public record Kpis(int days, long newRequests, long ordersCreated, long ordersCompleted,
                       BigDecimal avgHoursToMatch, BigDecimal matchedSharePercent,
                       BigDecimal onTimeDeliveryPercent, BigDecimal fulfilmentPercent, BigDecimal spoilagePercent,
                       BigDecimal valueTraded, BigDecimal quantityDelivered, BigDecimal paymentsReceivedPercent,
                       long activeShops, long activeFarmers, BigDecimal repeatShopsPercent, BigDecimal repeatFarmersPercent,
                       long openIssues, BigDecimal issueRatePercent, BigDecimal marketplaceSharePercent,
                       List<WeekKpi> weekly, List<DistrictKpi> districts) {}
}
