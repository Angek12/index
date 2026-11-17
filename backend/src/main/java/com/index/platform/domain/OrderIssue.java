package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "order_issues")
@Getter @Setter @NoArgsConstructor
public class OrderIssue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private PurchaseOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reported_by_user_id", nullable = false) private User reportedBy;
    @Enumerated(EnumType.STRING) @Column(name = "reporter_role", nullable = false, length = 10) private Role reporterRole;
    @Enumerated(EnumType.STRING) @Column(name = "issue_type", nullable = false, length = 30) private IssueType issueType;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "quantity_affected", precision = 12, scale = 2) private BigDecimal quantityAffected;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IssueStatus status = IssueStatus.OPEN;
    @Column(length = 500) private String resolution;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by_user_id") private User resolvedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "resolved_at") private Instant resolvedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
