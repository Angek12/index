package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.StandingOrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/standing-orders")
@PreAuthorize("hasRole('SHOP')")
@RequiredArgsConstructor
public class StandingOrderController {
    private final StandingOrderService service;

    @GetMapping
    public List<Dto.StandingOrderResponse> list(@AuthenticationPrincipal AuthUser u) { return service.list(u); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.StandingOrderResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.StandingOrderRequest r) {
        return service.create(u, r);
    }

    @PatchMapping("/{id}/active")
    public Dto.StandingOrderResponse setActive(@AuthenticationPrincipal AuthUser u, @PathVariable Long id, @RequestParam boolean value) {
        return service.setActive(u, id, value);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { service.delete(u, id); }
}
