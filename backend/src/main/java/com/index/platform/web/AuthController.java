package com.index.platform.web;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import com.index.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.AuthResponse register(@Valid @RequestBody Dto.RegisterRequest r) { return auth.register(r); }

    @PostMapping("/login")
    public Dto.AuthResponse login(@Valid @RequestBody Dto.LoginRequest r) { return auth.login(r); }

    @GetMapping("/me")
    public Dto.UserInfo me(@AuthenticationPrincipal AuthUser u) { return auth.me(u.id()); }

    @PutMapping("/me")
    public Dto.UserInfo update(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.ProfileUpdateRequest r) {
        return auth.updateProfile(u.id(), r);
    }

    @PostMapping("/verify-phone")
    public Dto.AuthResponse verifyPhone(@Valid @RequestBody Dto.VerifyPhoneRequest r) { return auth.verifyPhone(r); }

    @PostMapping("/resend-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendCode(@Valid @RequestBody Dto.ResendCodeRequest r) { auth.resendPhoneCode(r.email()); }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody Dto.ForgotPasswordRequest r) { auth.forgotPassword(r.email()); }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Dto.ResetPasswordRequest r) { auth.resetPassword(r); }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody Dto.ChangePasswordRequest r) {
        auth.changePassword(u.id(), r);
    }
}
