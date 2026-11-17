package com.index.platform.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "order_ratings")
@Getter @Setter @NoArgsConstructor
public class OrderRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private PurchaseOrder order;
    @Enumerated(EnumType.STRING) @Column(name = "rater_role", nullable = false, length = 10) private Role raterRole;
    @Column(nullable = false) private int stars;
    @Column(length = 300) private String comment;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
