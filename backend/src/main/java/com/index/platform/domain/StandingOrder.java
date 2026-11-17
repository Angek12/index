package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "standing_orders")
@Getter @Setter @NoArgsConstructor
public class StandingOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private ShopProfile shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "crop_id", nullable = false) private Crop crop;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(name = "max_price", precision = 12, scale = 2) private BigDecimal maxPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Frequency frequency;
    @Column(name = "next_needed_date", nullable = false) private LocalDate nextNeededDate;
    @Column(name = "lead_days", nullable = false) private int leadDays = 2;
    @Column(nullable = false) private boolean active = true;
    @Column(length = 500) private String notes;
    @Column(name = "last_run_on") private LocalDate lastRunOn;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
