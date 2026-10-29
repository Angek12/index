package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "sales_records")
@Getter @Setter @NoArgsConstructor
public class SalesRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private ShopProfile shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "crop_id", nullable = false) private Crop crop;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "sold_on", nullable = false) private LocalDate soldOn;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
