package com.index.platform.web;

import com.index.platform.domain.OrderStatus;
import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.IssueService;
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
    private final IssueService issueService;

    /** Admin matches a shop's demand request to a farmer's supply listing (one request can have several orders). */
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

    @PatchMapping("/{id}/payment")
    public Dto.OrderResponse payment(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                     @Valid @RequestBody Dto.PaymentRequest r) {
        return service.updatePayment(u, id, r);
    }

    @PatchMapping("/{id}/delivery")
    public Dto.OrderResponse delivery(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                      @Valid @RequestBody Dto.DeliveryRequest r) {
        return service.updateDelivery(u, id, r);
    }

    @PostMapping("/{id}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.IssueResponse reportIssue(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                         @Valid @RequestBody Dto.CreateIssueRequest r) {
        return issueService.create(u, id, r);
    }

    @GetMapping("/{id}/issues")
    public List<Dto.IssueResponse> issues(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return issueService.forOrder(u, id);
    }

    @PostMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.OrderResponse rate(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                  @Valid @RequestBody Dto.RatingRequest r) {
        return service.rate(u, id, r);
    }
}
