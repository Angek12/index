package com.index.platform.web;

import com.index.platform.domain.OrderStatus;
import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    /** Admin matches a shop's demand request to a farmer's supply listing. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.OrderResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.OrderCreateRequest r) {
        return service.create(u, r);
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dto.SupplySuggestion> suggestions(@RequestParam Long demandId) { return service.suggestions(demandId); }

    @GetMapping
    public List<Dto.OrderResponse> list(@AuthenticationPrincipal AuthUser u, @RequestParam(required = false) OrderStatus status) {
        return service.list(u, status);
    }

    @GetMapping("/{id}")
    public Dto.OrderResponse get(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.get(u, id); }

    @GetMapping("/{id}/history")
    public List<Dto.HistoryResponse> history(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.history(u, id);
    }

    @PatchMapping("/{id}/status")
    public Dto.OrderResponse updateStatus(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                          @Valid @RequestBody Dto.OrderStatusRequest r) {
        return service.updateStatus(u, id, r);
    }
}
