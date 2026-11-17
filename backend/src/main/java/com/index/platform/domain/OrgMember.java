package com.index.platform.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "org_members")
@Getter @Setter @NoArgsConstructor
public class OrgMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_type", nullable = false, length = 10) private String orgType;
    @Column(name = "org_id", nullable = false) private Long orgId;
    @Column(name = "user_id", nullable = false, unique = true) private Long userId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
