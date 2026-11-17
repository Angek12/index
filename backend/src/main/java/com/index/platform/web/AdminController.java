package com.index.platform.web;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.UserRepository;
import com.index.platform.security.AuthUser;
import com.index.platform.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final AuthService auth;
    private final ReportService reports;
    private final NotificationService notifier;
    private final AuditService audit;
    private final IssueService issueService;
    private final AttentionService attention;
    private final KpiService kpis;
    private final StandingOrderService standing;

    // ---------- users ----------
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Dto.UserInfo> users(@RequestParam(required = false) Role role,
                                    @RequestParam(required = false) ApprovalStatus approval) {
        List<User> list = approval != null ? users.findByApprovalStatusOrderByCreatedAtDesc(approval)
                : role == null ? users.findAllByOrderByCreatedAtDesc() : users.findByRoleOrderByCreatedAtDesc(role);
        return list.stream().filter(u -> role == null || u.getRole() == role).map(auth::info).toList();
    }

    @PatchMapping("/users/{id}/active")
    @Transactional
    public Dto.UserInfo setActive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestParam boolean value) {
        if (me.id().equals(id)) throw ApiException.badRequest("You cannot change your own active status");
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setActive(value);
        audit.log(me.id(), value ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id, u.getEmail());
        return auth.info(u);
    }

    @PatchMapping("/users/{id}/approve")
    @Transactional
    public Dto.UserInfo approve(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setApprovalStatus(ApprovalStatus.APPROVED);
        u.setActive(true);
        notifier.send(u, "ACCOUNT_APPROVED", Map.of());
        audit.log(me.id(), "USER_APPROVED", "USER", id, u.getEmail());
        return auth.info(u);
    }

    @PatchMapping("/users/{id}/reject")
    @Transactional
    public Dto.UserInfo reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        if (me.id().equals(id)) throw ApiException.badRequest("You cannot reject your own account");
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setApprovalStatus(ApprovalStatus.REJECTED);
        notifier.send(u, "ACCOUNT_REJECTED", Map.of());
        audit.log(me.id(), "USER_REJECTED", "USER", id, u.getEmail());
        return auth.info(u);
    }

    /** Confirms a phone number by hand, for example when SMS is not set up yet. */
    @PatchMapping("/users/{id}/verify-phone")
    public Dto.UserInfo verifyPhone(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return auth.adminVerifyPhone(me.id(), id);
    }

    /** One-time code the admin passes on to a user who forgot their password. */
    @PostMapping("/users/{id}/reset-code")
    public Dto.ResetCodeResponse resetCode(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return auth.adminResetCode(me.id(), id);
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.UserInfo createAdmin(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody Dto.CreateAdminRequest r) {
        return auth.createAdmin(me.id(), r);
    }

    // ---------- problem reports ----------
    @GetMapping("/issues")
    public List<Dto.IssueResponse> issues(@RequestParam(required = false) IssueStatus status) { return issueService.adminList(status); }

    @PatchMapping("/issues/{id}")
    public Dto.IssueResponse resolveIssue(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                          @Valid @RequestBody Dto.ResolveIssueRequest r) {
        return issueService.resolve(me, id, r);
    }

    // ---------- activity, attention, KPIs ----------
    @GetMapping("/audit")
    public List<Dto.AuditEntry> auditLog() { return audit.latest(); }

    @GetMapping("/attention")
    public Dto.Attention attention() { return attention.compute(); }

    @GetMapping("/kpis")
    public Dto.Kpis kpis(@RequestParam(defaultValue = "30") int days) { return kpis.compute(days); }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return reports.dashboard(); }

    /** For testing: creates today's repeating requests straight away. */
    @PostMapping("/jobs/repeating-requests")
    public Map<String, Integer> runRepeating(@AuthenticationPrincipal AuthUser me) {
        int n = standing.runDue(LocalDate.now());
        audit.log(me.id(), "REPEATING_REQUESTS_RUN", null, null, "created " + n);
        return Map.of("created", n);
    }

    @PostMapping("/jobs/digest")
    public Map<String, String> runDigest() {
        attention.sendDigest();
        return Map.of("status", "sent if anything needs attention");
    }
}
