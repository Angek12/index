package com.index.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Switches read from environment variables. */
@Component
public class AppSettings {
    private final boolean requireApproval;
    private final boolean requirePhoneVerification;
    private final boolean marketplaceEnabled;
    private final boolean requireDeliveryCode;

    public AppSettings(@Value("${app.require-approval:true}") boolean requireApproval,
                       @Value("${app.require-phone-verification:false}") boolean requirePhoneVerification,
                       @Value("${app.marketplace-enabled:true}") boolean marketplaceEnabled,
                       @Value("${app.require-delivery-code:true}") boolean requireDeliveryCode) {
        this.requireApproval = requireApproval;
        this.requirePhoneVerification = requirePhoneVerification;
        this.marketplaceEnabled = marketplaceEnabled;
        this.requireDeliveryCode = requireDeliveryCode;
    }

    public boolean requireApproval() { return requireApproval; }
    public boolean requirePhoneVerification() { return requirePhoneVerification; }
    public boolean marketplaceEnabled() { return marketplaceEnabled; }
    public boolean requireDeliveryCode() { return requireDeliveryCode; }
}
