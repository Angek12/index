package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "farmer_profiles")
@Getter @Setter @NoArgsConstructor
public class FarmerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(name = "farm_name", nullable = false, length = 150) private String farmName;
    @Column(length = 80) private String district;
    @Column(length = 80) private String sector;
    @Column(length = 255) private String address;
}
