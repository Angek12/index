package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Finds the shop or farm a user belongs to, either as owner or as a team member. */
@Component
@RequiredArgsConstructor
public class ProfileResolver {
    private final ShopProfileRepository shops;
    private final FarmerProfileRepository farmers;
    private final OrgMemberRepository members;

    public Optional<ShopProfile> shopForUser(Long userId) {
        return shops.findByUserId(userId).or(() -> members.findByUserId(userId)
                .filter(m -> "SHOP".equals(m.getOrgType())).flatMap(m -> shops.findById(m.getOrgId())));
    }

    public Optional<FarmerProfile> farmerForUser(Long userId) {
        return farmers.findByUserId(userId).or(() -> members.findByUserId(userId)
                .filter(m -> "FARMER".equals(m.getOrgType())).flatMap(m -> farmers.findById(m.getOrgId())));
    }

    public ShopProfile shop(AuthUser u) {
        return shopForUser(u.id()).orElseThrow(() -> ApiException.forbidden("A shop profile is required"));
    }

    public FarmerProfile farmer(AuthUser u) {
        return farmerForUser(u.id()).orElseThrow(() -> ApiException.forbidden("A farmer profile is required"));
    }

    public boolean isShopSide(AuthUser u, ShopProfile p) {
        return p.getUser().getId().equals(u.id()) || members.existsByOrgTypeAndOrgIdAndUserId("SHOP", p.getId(), u.id());
    }

    public boolean isFarmerSide(AuthUser u, FarmerProfile p) {
        return p.getUser().getId().equals(u.id()) || members.existsByOrgTypeAndOrgIdAndUserId("FARMER", p.getId(), u.id());
    }
}
