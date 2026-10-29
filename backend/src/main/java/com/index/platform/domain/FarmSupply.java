package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "farm_supplies")
@Getter @Setter @NoArgsConstructor
public class FarmSupply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "farmer_id", nullable = false) private FarmerProfile farmer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "crop_id", nullable = false) private Crop crop;
    @Column(name = "quantity_available", nullable = false, precision = 12, scale = 2) private BigDecimal quantityAvailable;
    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2) private BigDecimal pricePerUnit;
    @Column(name = "available_from", nullable = false) private LocalDate availableFrom;
    @Column(name = "available_until") private LocalDate availableUntil;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SupplyStatus status = SupplyStatus.AVAILABLE;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
