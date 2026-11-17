package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndSeenFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.seen = true where n.user.id = :uid and n.seen = false")
    int markAllSeen(@Param("uid") Long uid);
}
