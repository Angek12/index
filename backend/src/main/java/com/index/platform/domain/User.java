package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(length = 30) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    @Enumerated(EnumType.STRING) @Column(name = "approval_status", nullable = false, length = 20) private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;
    @Column(nullable = false, length = 5) private String language = "en";
    @Column(name = "notify_sms", nullable = false) private boolean notifySms = true;
    @Column(name = "notify_whatsapp", nullable = false) private boolean notifyWhatsapp = false;

    @Column(name = "phone_verified", nullable = false) private boolean phoneVerified = false;
    @Column(name = "terms_accepted_at") private Instant termsAcceptedAt;
}
