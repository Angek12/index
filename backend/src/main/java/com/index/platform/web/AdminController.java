package com.index.platform.web;

import com.index.platform.domain.Role;
import com.index.platform.domain.User;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.UserRepository;
import com.index.platform.security.AuthUser;
import com.index.platform.service.AuthService;
import com.index.platform.service.ReportService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Dto.UserInfo> users(@RequestParam(required = false) Role role) {
        List<User> list = role == null ? users.findAllByOrderByCreatedAtDesc() : users.findByRoleOrderByCreatedAtDesc(role);
        return list.stream().map(auth::info).toList();
    }

    @PatchMapping("/users/{id}/active")
    @Transactional
    public Dto.UserInfo setActive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestParam boolean value) {
        if (me.id().equals(id)) throw ApiException.badRequest("You cannot change your own active status");
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setActive(value);
        return auth.info(u);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return reports.dashboard(); }
}
