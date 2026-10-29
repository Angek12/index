package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "demand_requests")
@Getter @Setter @NoArgsConstructor
public class DemandRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private ShopProfile shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "crop_id", nullable = false) private Crop crop;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(name = "max_price", precision = 12, scale = 2) private BigDecimal maxPrice;
    @Column(name = "needed_by", nullable = false) private LocalDate neededBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DemandStatus status = DemandStatus.OPEN;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
