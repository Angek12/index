package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileResolver {
    private final ShopProfileRepository shops;
    private final FarmerProfileRepository farmers;

    public ShopProfile shop(AuthUser u) {
        return shops.findByUserId(u.id()).orElseThrow(() -> ApiException.forbidden("A shop profile is required"));
    }

    public FarmerProfile farmer(AuthUser u) {
        return farmers.findByUserId(u.id()).orElseThrow(() -> ApiException.forbidden("A farmer profile is required"));
    }
}
