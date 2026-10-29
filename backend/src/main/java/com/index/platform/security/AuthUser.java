package com.index.platform.security;

import com.index.platform.domain.Role;

public record AuthUser(Long id, String email, Role role) {}
