package com.index.platform.service;

import com.index.platform.config.AppSettings;
import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.JwtService;
import com.index.platform.util.PhoneUtil;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    public static final String PHONE_NOT_VERIFIED = "Please confirm your phone number with the code we sent you";

    private final UserRepository users;
    private final ShopProfileRepository shops;
    private final FarmerProfileRepository farmers;
    private final PasswordResetRepository resets;
    private final PhoneVerificationRepository phoneCodes;
    private final ProfileResolver resolver;
    private final NotificationService notifier;
    private final AuditService audit;
    private final LoginGuard loginGuard;
    private final AppSettings settings;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SecureRandom random = new SecureRandom();

    private static String lang(String l) { return "rw".equalsIgnoreCase(l) ? "rw" : "en"; }

    // ---------- sign up ----------
    @Transactional
    public Dto.AuthResponse register(Dto.RegisterRequest r) {
        if (r.role() == Role.ADMIN) throw ApiException.badRequest("Role must be SHOP or FARMER");
        if (!Boolean.TRUE.equals(r.acceptTerms())) throw ApiException.badRequest("You must accept the terms to create an account");
        String phone = PhoneUtil.normalize(r.phone());
        boolean verify = settings.requirePhoneVerification();
        if (verify && phone == null) throw ApiException.badRequest("A phone number is required");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email is already registered");

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhone(phone);
        u.setRole(r.role());
        u.setLanguage(lang(r.language()));
        u.setTermsAcceptedAt(Instant.now());
        u.setPhoneVerified(!verify);
        u.setApprovalStatus(settings.requireApproval() ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED);
        users.save(u);

        if (r.role() == Role.SHOP) {
            ShopProfile p = new ShopProfile();
            p.setUser(u); p.setShopName(r.businessName().trim());
            p.setDistrict(r.district()); p.setSector(r.sector()); p.setAddress(r.address());
            shops.save(p);
        } else {
            FarmerProfile p = new FarmerProfile();
            p.setUser(u); p.setFarmName(r.businessName().trim());
            p.setDistrict(r.district()); p.setSector(r.sector()); p.setAddress(r.address());
            p.setCooperative(Boolean.TRUE.equals(r.cooperative()));
            farmers.save(p);
        }

        if (u.getApprovalStatus() == ApprovalStatus.PENDING) {
            notifier.notifyAdmins("NEW_ACCOUNT_PENDING", Map.of("role", u.getRole().name().toLowerCase(), "name", r.businessName().trim()));
        }
        if (verify) {
            sendPhoneCode(u);
            return new Dto.AuthResponse(null, info(u), "We sent a 6-digit code to your phone. Enter it to confirm your number.", true);
        }
        return finish(u);
    }

    /** Signs the person in, or explains what they are still waiting for. */
    private Dto.AuthResponse finish(User u) {
        if (u.getApprovalStatus() == ApprovalStatus.PENDING)
            return new Dto.AuthResponse(null, info(u), "Your account was created and is waiting for admin approval.", false);
        return new Dto.AuthResponse(jwt.generate(u), info(u), null, false);
    }

    // ---------- sign in ----------
    @Transactional(readOnly = true)
    public Dto.AuthResponse login(Dto.LoginRequest r) {
        String key = r.email().trim().toLowerCase();
        loginGuard.check(key);
        Optional<User> found = users.findByEmailIgnoreCase(key);
        if (found.isEmpty() || !encoder.matches(r.password(), found.get().getPasswordHash())) {
            loginGuard.failure(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User u = found.get();
        loginGuard.success(key);
        if (settings.requirePhoneVerification() && !u.isPhoneVerified()) throw ApiException.forbidden(PHONE_NOT_VERIFIED);
        if (u.getApprovalStatus() == ApprovalStatus.PENDING) throw ApiException.forbidden("Your account is waiting for admin approval");
        if (u.getApprovalStatus() == ApprovalStatus.REJECTED) throw ApiException.forbidden("Your account request was not approved");
        if (!u.isActive()) throw ApiException.forbidden("This account is deactivated");
        return new Dto.AuthResponse(jwt.generate(u), info(u), null, false);
    }

    @Transactional(readOnly = true)
    public Dto.UserInfo me(Long userId) {
        return info(users.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
    }

    @Transactional
    public Dto.UserInfo updateProfile(Long userId, Dto.ProfileUpdateRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (r.fullName() != null && !r.fullName().isBlank()) u.setFullName(r.fullName().trim());
        if (r.phone() != null) {
            String phone = PhoneUtil.normalize(r.phone());
            if (phone != null && !phone.equals(u.getPhone())) u.setPhoneVerified(!settings.requirePhoneVerification());
            u.setPhone(phone);
        }
        if (r.language() != null) u.setLanguage(lang(r.language()));
        if (r.notifySms() != null) u.setNotifySms(r.notifySms());
        if (r.notifyWhatsapp() != null) u.setNotifyWhatsapp(r.notifyWhatsapp());
        if (u.getRole() == Role.SHOP) {
            ShopProfile p = resolver.shopForUser(userId).orElseThrow(() -> ApiException.notFound("Shop profile"));
            if (p.getUser().getId().equals(userId)) {
                if (r.businessName() != null && !r.businessName().isBlank()) p.setShopName(r.businessName().trim());
                if (r.district() != null) p.setDistrict(r.district());
                if (r.sector() != null) p.setSector(r.sector());
                if (r.address() != null) p.setAddress(r.address());
            }
        } else if (u.getRole() == Role.FARMER) {
            FarmerProfile p = resolver.farmerForUser(userId).orElseThrow(() -> ApiException.notFound("Farmer profile"));
            if (p.getUser().getId().equals(userId)) {
                if (r.businessName() != null && !r.businessName().isBlank()) p.setFarmName(r.businessName().trim());
                if (r.district() != null) p.setDistrict(r.district());
                if (r.sector() != null) p.setSector(r.sector());
                if (r.address() != null) p.setAddress(r.address());
            }
        }
        return info(u);
    }

    // ---------- phone verification ----------
    private String newCode() { return String.format("%06d", random.nextInt(1_000_000)); }

    private void sendPhoneCode(User u) {
        String code = newCode();
        PhoneVerification pv = new PhoneVerification();
        pv.setUser(u);
        pv.setCodeHash(encoder.encode(code));
        pv.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        phoneCodes.save(pv);
        notifier.sendSmsOnly(u, "PHONE_CODE", Map.of("code", code));
    }

    /** Sends a new code. Always succeeds silently so nobody can find out which emails exist. */
    @Transactional
    public void resendPhoneCode(String email) {
        Optional<User> found = users.findByEmailIgnoreCase(email.trim()).filter(x -> x.isActive() && !x.isPhoneVerified() && x.getPhone() != null);
        if (found.isEmpty()) return;
        User u = found.get();
        Optional<PhoneVerification> last = phoneCodes.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.getId());
        if (last.isPresent() && last.get().getCreatedAt().isAfter(Instant.now().minusSeconds(60))) return;
        sendPhoneCode(u);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Dto.AuthResponse verifyPhone(Dto.VerifyPhoneRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        PhoneVerification pv = phoneCodes.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.getId())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        if (pv.getExpiresAt().isBefore(Instant.now()) || pv.getAttempts() >= 5) throw ApiException.badRequest("Invalid or expired code");
        if (!encoder.matches(r.code().trim(), pv.getCodeHash())) {
            pv.setAttempts(pv.getAttempts() + 1);
            throw ApiException.badRequest("Invalid or expired code");
        }
        pv.setUsed(true);
        u.setPhoneVerified(true);
        if (u.getApprovalStatus() == ApprovalStatus.PENDING)
            return new Dto.AuthResponse(null, info(u), "Phone confirmed. Your account is waiting for admin approval.", false);
        if (u.getApprovalStatus() == ApprovalStatus.REJECTED || !u.isActive()) throw ApiException.forbidden("This account cannot sign in");
        return new Dto.AuthResponse(jwt.generate(u), info(u), null, false);
    }

    @Transactional
    public Dto.UserInfo adminVerifyPhone(Long actorId, Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        u.setPhoneVerified(true);
        audit.log(actorId, "PHONE_VERIFIED", "USER", userId, u.getEmail());
        return info(u);
    }

    // ---------- admins ----------
    @Transactional
    public Dto.UserInfo createAdmin(Long actorId, Dto.CreateAdminRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email is already registered");
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhone(PhoneUtil.normalize(r.phone()));
        u.setRole(Role.ADMIN);
        u.setApprovalStatus(ApprovalStatus.APPROVED);
        u.setPhoneVerified(true);
        u.setTermsAcceptedAt(Instant.now());
        users.save(u);
        audit.log(actorId, "ADMIN_CREATED", "USER", u.getId(), email);
        return info(u);
    }

    // ---------- passwords ----------
    @Transactional
    public void changePassword(Long userId, Dto.ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw ApiException.badRequest("Current password is wrong");
        u.setPasswordHash(encoder.encode(r.newPassword()));
    }

    private String issueCode(User u, long minutes) {
        String code = newCode();
        PasswordReset pr = new PasswordReset();
        pr.setUser(u);
        pr.setCodeHash(encoder.encode(code));
        pr.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        resets.save(pr);
        return code;
    }

    /** Always succeeds silently so nobody can find out which emails exist. */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> found = users.findByEmailIgnoreCase(email.trim()).filter(User::isActive);
        if (found.isEmpty()) return;
        User u = found.get();
        Optional<PasswordReset> last = resets.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.getId());
        if (last.isPresent() && last.get().getCreatedAt().isAfter(Instant.now().minusSeconds(60))) return;
        String code = issueCode(u, 30);
        notifier.sendSmsOnly(u, "PASSWORD_RESET_CODE", Map.of("code", code));
    }

    /** Admin generates a one-time code and gives it to the user by phone or WhatsApp. */
    @Transactional
    public Dto.ResetCodeResponse adminResetCode(Long actorId, Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        String code = issueCode(u, 24 * 60);
        audit.log(actorId, "RESET_CODE_ISSUED", "USER", userId, u.getEmail());
        return new Dto.ResetCodeResponse(code, Instant.now().plus(24, ChronoUnit.HOURS));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(Dto.ResetPasswordRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        PasswordReset pr = resets.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.getId())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        if (pr.getExpiresAt().isBefore(Instant.now()) || pr.getAttempts() >= 5) throw ApiException.badRequest("Invalid or expired code");
        if (!encoder.matches(r.code().trim(), pr.getCodeHash())) {
            pr.setAttempts(pr.getAttempts() + 1);
            throw ApiException.badRequest("Invalid or expired code");
        }
        u.setPasswordHash(encoder.encode(r.newPassword()));
        pr.setUsed(true);
    }

    // ---------- profile info ----------
    public Dto.UserInfo info(User u) {
        Long profileId = null;
        String name = null, district = null, sector = null, address = null, teamRole = null;
        boolean coop = false;
        if (u.getRole() == Role.SHOP) {
            Optional<ShopProfile> p = resolver.shopForUser(u.getId());
            if (p.isPresent()) {
                ShopProfile s = p.get();
                profileId = s.getId(); name = s.getShopName(); district = s.getDistrict(); sector = s.getSector(); address = s.getAddress();
                teamRole = s.getUser().getId().equals(u.getId()) ? "OWNER" : "MEMBER";
            }
        } else if (u.getRole() == Role.FARMER) {
            Optional<FarmerProfile> p = resolver.farmerForUser(u.getId());
            if (p.isPresent()) {
                FarmerProfile f = p.get();
                profileId = f.getId(); name = f.getFarmName(); district = f.getDistrict(); sector = f.getSector(); address = f.getAddress();
                coop = f.isCooperative();
                teamRole = f.getUser().getId().equals(u.getId()) ? "OWNER" : "MEMBER";
            }
        }
        return new Dto.UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.isActive(),
                profileId, name, district, sector, address, u.getLanguage(), u.isNotifySms(), u.isNotifyWhatsapp(),
                u.getApprovalStatus(), coop, teamRole, u.isPhoneVerified());
    }
}
