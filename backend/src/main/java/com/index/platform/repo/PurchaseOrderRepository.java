package com.index.platform.repo;

import com.index.platform.domain.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<PurchaseOrder> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<PurchaseOrder> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    @Query("select sum(o.quantity) from PurchaseOrder o where o.demandRequest.id = :demandId and o.status in :statuses")
    BigDecimal sumQuantity(@Param("demandId") Long demandId, @Param("statuses") Collection<OrderStatus> statuses);
}
