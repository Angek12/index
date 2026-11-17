package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderIssueRepository extends JpaRepository<OrderIssue, Long> {
    List<OrderIssue> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<OrderIssue> findByStatusOrderByCreatedAtDesc(IssueStatus status);
    List<OrderIssue> findAllByOrderByCreatedAtDesc();
    long countByOrderIdAndStatus(Long orderId, IssueStatus status);
    long countByStatus(IssueStatus status);
}
