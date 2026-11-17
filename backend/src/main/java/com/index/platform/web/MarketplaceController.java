package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.OrderService;
import com.index.platform.service.SupplyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Shops browse farmer listings and order directly. The farmer then confirms or declines. */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {
    private final SupplyService supplies;
    private final OrderService orders;

    @GetMapping("/listings")
    @PreAuthorize("hasAnyRole('SHOP','ADMIN')")
    public List<Dto.SupplyResponse> listings(@RequestParam(required = false) Long cropId,
                                             @RequestParam(required = false) String district) {
        return supplies.marketListings(cropId, district);
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('SHOP')")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.OrderResponse order(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.MarketOrderRequest r) {
        return orders.marketplaceOrder(u, r);
    }
}
