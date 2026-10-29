package com.index.platform.repo;

import com.index.platform.domain.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<PurchaseOrder> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<PurchaseOrder> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
}
