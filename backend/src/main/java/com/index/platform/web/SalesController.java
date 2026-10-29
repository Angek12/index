package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.SalesService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasRole('SHOP')")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.SalesResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.SalesRequest r) {
        return service.create(u, r);
    }

    @GetMapping
    public List<Dto.SalesResponse> list(@AuthenticationPrincipal AuthUser u,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(u, from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { service.delete(u, id); }
}
