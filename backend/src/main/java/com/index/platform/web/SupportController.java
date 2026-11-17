package com.index.platform.web;

import com.index.platform.domain.TicketStatus;
import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.SupportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Users see their own conversations. Admins see all of them. */
@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportController {
    private final SupportService service;

    @GetMapping
    public List<Dto.TicketSummary> list(@AuthenticationPrincipal AuthUser u, @RequestParam(required = false) TicketStatus status) {
        return service.list(u, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.TicketDetail create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.TicketCreateRequest r) {
        return service.create(u, r);
    }

    @GetMapping("/{id}")
    public Dto.TicketDetail detail(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.detail(u, id); }

    @PostMapping("/{id}/messages")
    public Dto.TicketDetail reply(@AuthenticationPrincipal AuthUser u, @PathVariable Long id, @Valid @RequestBody Dto.TicketMessageRequest r) {
        return service.reply(u, id, r);
    }

    @PatchMapping("/{id}/close")
    public Dto.TicketDetail close(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.close(u, id); }
}
