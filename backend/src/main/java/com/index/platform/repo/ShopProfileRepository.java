package com.index.platform.repo;

import com.index.platform.domain.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ShopProfileRepository extends JpaRepository<ShopProfile, Long> {
    Optional<ShopProfile> findByUserId(Long userId);
}
