package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team")
@PreAuthorize("hasAnyRole('SHOP','FARMER')")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService service;

    @GetMapping
    public List<Dto.TeamMember> list(@AuthenticationPrincipal AuthUser u) { return service.list(u); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.TeamMember add(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.AddMemberRequest r) {
        return service.add(u, r);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser u, @PathVariable Long userId) { service.remove(u, userId); }
}
