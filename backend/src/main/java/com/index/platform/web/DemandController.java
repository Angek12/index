package com.index.platform.web;

import com.index.platform.domain.DemandStatus;
import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.DemandService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {
    private final DemandService service;

    @PostMapping
    @PreAuthorize("hasRole('SHOP')")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.DemandResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.DemandCreateRequest r) {
        return service.create(u, r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SHOP')")
    public List<Dto.DemandResponse> mine(@AuthenticationPrincipal AuthUser u) { return service.mine(u); }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dto.DemandResponse> all(@RequestParam(required = false) DemandStatus status) { return service.all(status); }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SHOP','ADMIN')")
    public Dto.DemandResponse cancel(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.cancel(u, id);
    }
}
