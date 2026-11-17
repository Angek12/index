package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Help messages between users and the Index team. */
@Service
@RequiredArgsConstructor
public class SupportService {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final UserRepository users;
    private final NotificationService notifier;

    private Dto.TicketSummary summary(SupportTicket t) {
        return new Dto.TicketSummary(t.getId(), t.getSubject(), t.getCategory(), t.getStatus(), t.getUser().getFullName(),
                t.getUser().getRole().name(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private SupportTicket find(AuthUser u, Long id) {
        SupportTicket t = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Support message"));
        if (u.role() != Role.ADMIN && !t.getUser().getId().equals(u.id())) throw ApiException.forbidden("This conversation belongs to someone else");
        return t;
    }

    private SupportMessage addMessage(SupportTicket t, AuthUser u, String body) {
        SupportMessage m = new SupportMessage();
        m.setTicket(t);
        m.setSender(users.getReferenceById(u.id()));
        m.setSenderRole(u.role());
        m.setBody(body.trim());
        return messages.save(m);
    }

    @Transactional
    public Dto.TicketDetail create(AuthUser u, Dto.TicketCreateRequest r) {
        SupportTicket t = new SupportTicket();
        t.setUser(users.getReferenceById(u.id()));
        t.setSubject(r.subject().trim());
        t.setCategory(r.category());
        t.setStatus(TicketStatus.OPEN);
        tickets.save(t);
        addMessage(t, u, r.message());
        notifier.notifyAdmins("SUPPORT_NEW", Map.of("name", t.getUser().getFullName(), "subject", t.getSubject()));
        return detail(u, t.getId());
    }

    @Transactional(readOnly = true)
    public List<Dto.TicketSummary> list(AuthUser u, TicketStatus status) {
        List<SupportTicket> list;
        if (u.role() == Role.ADMIN) list = status == null ? tickets.findAllByOrderByUpdatedAtDesc() : tickets.findByStatusOrderByUpdatedAtDesc(status);
        else list = tickets.findByUserIdOrderByUpdatedAtDesc(u.id());
        return list.stream().filter(t -> status == null || t.getStatus() == status).map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public Dto.TicketDetail detail(AuthUser u, Long id) {
        SupportTicket t = find(u, id);
        List<Dto.TicketMessageResponse> msgs = messages.findByTicketIdOrderByCreatedAtAsc(id).stream()
                .map(m -> new Dto.TicketMessageResponse(m.getId(), m.getSender().getFullName(), m.getSenderRole(), m.getBody(), m.getCreatedAt()))
                .toList();
        return new Dto.TicketDetail(summary(t), msgs);
    }

    @Transactional
    public Dto.TicketDetail reply(AuthUser u, Long id, Dto.TicketMessageRequest r) {
        SupportTicket t = find(u, id);
        addMessage(t, u, r.body());
        if (u.role() == Role.ADMIN) {
            t.setStatus(TicketStatus.ANSWERED);
            if (!t.getUser().getId().equals(u.id())) notifier.send(t.getUser(), "SUPPORT_REPLY", Map.of("subject", t.getSubject()));
        } else {
            t.setStatus(TicketStatus.OPEN);
            notifier.notifyAdmins("SUPPORT_NEW", Map.of("name", t.getUser().getFullName(), "subject", t.getSubject()));
        }
        t.setUpdatedAt(java.time.Instant.now());
        return detail(u, id);
    }

    @Transactional
    public Dto.TicketDetail close(AuthUser u, Long id) {
        SupportTicket t = find(u, id);
        t.setStatus(TicketStatus.CLOSED);
        return detail(u, id);
    }
}
