package com.index.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.index.platform.domain.*;
import com.index.platform.security.JwtService;
import com.index.platform.service.OrderService;
import com.index.platform.util.PhoneUtil;
import org.junit.jupiter.api.Test;

class CoreLogicTest {

    @Test
    void jwtRoundTrip() {
        JwtService jwt = new JwtService("0123456789012345678901234567890123456789", 60);
        User u = new User();
        u.setId(7L);
        u.setRole(Role.SHOP);
        assertEquals(7L, jwt.parseUserId(jwt.generate(u)));
    }

    @Test
    void orderTransitions() {
        assertTrue(OrderService.canTransition(OrderStatus.MATCHED, OrderStatus.CONFIRMED));
        assertTrue(OrderService.canTransition(OrderStatus.DELIVERED, OrderStatus.COMPLETED));
        assertFalse(OrderService.canTransition(OrderStatus.MATCHED, OrderStatus.COMPLETED));
        assertFalse(OrderService.canTransition(OrderStatus.COMPLETED, OrderStatus.FAILED));
        assertFalse(OrderService.canTransition(OrderStatus.CANCELLED, OrderStatus.MATCHED));
    }

    @Test
    void phoneNumbersAreNormalised() {
        assertEquals("+250795288755", PhoneUtil.normalize("0795 288 755"));
        assertEquals("+250795288755", PhoneUtil.normalize("250795288755"));
        assertEquals("+250795288755", PhoneUtil.normalize("+250795288755"));
        assertTrue(PhoneUtil.variants("+250795288755").contains("0795288755"));
    }
}
