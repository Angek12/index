package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRatingRepository extends JpaRepository<OrderRating, Long> {
    List<OrderRating> findByOrderId(Long orderId);
    boolean existsByOrderIdAndRaterRole(Long orderId, Role raterRole);
}
