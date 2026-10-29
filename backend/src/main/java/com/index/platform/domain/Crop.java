package com.index.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "crops")
@Getter @Setter @NoArgsConstructor
public class Crop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(length = 50) private String category;
    @Column(nullable = false, length = 20) private String unit = "kg";
    @Column(nullable = false) private boolean active = true;
}
