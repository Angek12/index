package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "order_status_history")
@Getter @Setter @NoArgsConstructor
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private PurchaseOrder order;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 20) private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 20) private OrderStatus toStatus;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by_user_id") private User changedBy;
    @Column(length = 500) private String note;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;

    @PrePersist void onCreate() { if (changedAt == null) changedAt = Instant.now(); }
}
