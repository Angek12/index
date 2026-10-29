package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final ShopProfileRepository shops;
    private final FarmerProfileRepository farmers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public Dto.AuthResponse register(Dto.RegisterRequest r) {
        if (r.role() == Role.ADMIN) throw ApiException.badRequest("Role must be SHOP or FARMER");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email is already registered");

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhone(r.phone());
        u.setRole(r.role());
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
            farmers.save(p);
        }
        return new Dto.AuthResponse(jwt.generate(u), info(u));
    }

    @Transactional(readOnly = true)
    public Dto.AuthResponse login(Dto.LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        if (!u.isActive()) throw ApiException.forbidden("This account is deactivated");
        return new Dto.AuthResponse(jwt.generate(u), info(u));
    }

    @Transactional(readOnly = true)
    public Dto.UserInfo me(Long userId) {
        return info(users.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
    }

    @Transactional
    public Dto.UserInfo updateProfile(Long userId, Dto.ProfileUpdateRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (r.fullName() != null && !r.fullName().isBlank()) u.setFullName(r.fullName().trim());
        if (r.phone() != null) u.setPhone(r.phone());
        if (u.getRole() == Role.SHOP) {
            ShopProfile p = shops.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Shop profile"));
            if (r.businessName() != null && !r.businessName().isBlank()) p.setShopName(r.businessName().trim());
            if (r.district() != null) p.setDistrict(r.district());
            if (r.sector() != null) p.setSector(r.sector());
            if (r.address() != null) p.setAddress(r.address());
        } else if (u.getRole() == Role.FARMER) {
            FarmerProfile p = farmers.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Farmer profile"));
            if (r.businessName() != null && !r.businessName().isBlank()) p.setFarmName(r.businessName().trim());
            if (r.district() != null) p.setDistrict(r.district());
            if (r.sector() != null) p.setSector(r.sector());
            if (r.address() != null) p.setAddress(r.address());
        }
        return info(u);
    }

    public Dto.UserInfo info(User u) {
        if (u.getRole() == Role.SHOP) {
            return shops.findByUserId(u.getId())
                    .map(p -> new Dto.UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                            u.isActive(), p.getId(), p.getShopName(), p.getDistrict(), p.getSector(), p.getAddress()))
                    .orElseGet(() -> plain(u));
        }
        if (u.getRole() == Role.FARMER) {
            return farmers.findByUserId(u.getId())
                    .map(p -> new Dto.UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                            u.isActive(), p.getId(), p.getFarmName(), p.getDistrict(), p.getSector(), p.getAddress()))
                    .orElseGet(() -> plain(u));
        }
        return plain(u);
    }

    private Dto.UserInfo plain(User u) {
        return new Dto.UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                u.isActive(), null, null, null, null, null);
    }
}
