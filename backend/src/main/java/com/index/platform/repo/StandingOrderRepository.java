package com.index.platform.repo;

import com.index.platform.domain.StandingOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingOrderRepository extends JpaRepository<StandingOrder, Long> {
    List<StandingOrder> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<StandingOrder> findByActiveTrue();
}
