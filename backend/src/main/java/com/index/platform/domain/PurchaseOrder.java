package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "demand_request_id", nullable = false) private DemandRequest demandRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farm_supply_id", nullable = false) private FarmSupply farmSupply;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private ShopProfile shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farmer_id", nullable = false) private FarmerProfile farmer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "crop_id", nullable = false) private Crop crop;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(name = "agreed_price", nullable = false, precision = 12, scale = 2) private BigDecimal agreedPrice;
    @Column(name = "delivery_date", nullable = false) private LocalDate deliveryDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status = OrderStatus.MATCHED;
    @Column(name = "transport_cost", precision = 12, scale = 2) private BigDecimal transportCost;
    @Column(name = "spoiled_quantity", precision = 12, scale = 2) private BigDecimal spoiledQuantity;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
