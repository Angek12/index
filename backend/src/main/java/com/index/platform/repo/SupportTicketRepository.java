package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<SupportTicket> findByStatusOrderByUpdatedAtDesc(TicketStatus status);
    List<SupportTicket> findAllByOrderByUpdatedAtDesc();
    long countByStatus(TicketStatus status);
}
