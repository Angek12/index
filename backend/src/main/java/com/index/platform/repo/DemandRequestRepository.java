package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandRequestRepository extends JpaRepository<DemandRequest, Long> {
    List<DemandRequest> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<DemandRequest> findByStatusOrderByNeededByAsc(DemandStatus status);
    List<DemandRequest> findByStatusInOrderByNeededByAsc(Collection<DemandStatus> statuses);
    List<DemandRequest> findAllByOrderByCreatedAtDesc();
}
