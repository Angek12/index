package com.index.platform.web;

import com.index.platform.domain.SupplyStatus;
import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.SupplyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
public class SupplyController {
    private final SupplyService service;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.SupplyResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.SupplyCreateRequest r) {
        return service.create(u, r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<Dto.SupplyResponse> mine(@AuthenticationPrincipal AuthUser u) { return service.mine(u); }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dto.SupplyResponse> all(@RequestParam(required = false) SupplyStatus status) { return service.all(status); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    public Dto.SupplyResponse update(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                     @Valid @RequestBody Dto.SupplyUpdateRequest r) {
        return service.update(u, id, r);
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    public Dto.SupplyResponse withdraw(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.withdraw(u, id);
    }
}
