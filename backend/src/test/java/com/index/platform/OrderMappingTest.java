package com.index.platform;

import static org.junit.jupiter.api.Assertions.*;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.service.Mappers;
import com.index.platform.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks who may see phone numbers and the delivery code, and the order step rules. */
class OrderMappingTest {

    private PurchaseOrder order(OrderStatus status) {
        User shopUser = new User(); shopUser.setId(1L); shopUser.setFullName("Shop Owner"); shopUser.setPhone("+250780000001");
        User farmUser = new User(); farmUser.setId(2L); farmUser.setFullName("Farm Owner"); farmUser.setPhone("+250780000002");
        ShopProfile shop = new ShopProfile(); shop.setId(10L); shop.setUser(shopUser); shop.setShopName("Shop"); shop.setDistrict("Gasabo");
        FarmerProfile farm = new FarmerProfile(); farm.setId(20L); farm.setUser(farmUser); farm.setFarmName("Farm"); farm.setDistrict("Musanze");
        Crop crop = new Crop(); crop.setId(30L); crop.setName("Tomatoes"); crop.setUnit("kg");
        DemandRequest d = new DemandRequest(); d.setId(40L);
        FarmSupply s = new FarmSupply(); s.setId(50L);
        PurchaseOrder o = new PurchaseOrder();
        o.setId(60L); o.setShop(shop); o.setFarmer(farm); o.setCrop(crop); o.setDemandRequest(d); o.setFarmSupply(s);
        o.setQuantity(new BigDecimal("50")); o.setAgreedPrice(new BigDecimal("800")); o.setDeliveryDate(LocalDate.now());
        o.setStatus(status); o.setDeliveryCode("4821");
        return o;
    }

    @Test
    void shopSeesFarmerContactAndDeliveryCode() {
        Dto.OrderResponse r = Mappers.order(order(OrderStatus.CONFIRMED), Role.SHOP, List.of(), 0);
        assertEquals("+250780000002", r.farmerPhone());
        assertNull(r.shopPhone());
        assertEquals("4821", r.deliveryCode());
        assertTrue(r.deliveryCodeRequired());
    }

    @Test
    void farmerSeesShopContactButNotTheDeliveryCode() {
        Dto.OrderResponse r = Mappers.order(order(OrderStatus.CONFIRMED), Role.FARMER, List.of(), 0);
        assertEquals("+250780000001", r.shopPhone());
        assertNull(r.farmerPhone());
        assertNull(r.deliveryCode());
        assertTrue(r.deliveryCodeRequired());
    }

    @Test
    void adminSeesBothContacts() {
        Dto.OrderResponse r = Mappers.order(order(OrderStatus.MATCHED), Role.ADMIN, List.of(), 2);
        assertNotNull(r.shopPhone());
        assertNotNull(r.farmerPhone());
        assertEquals(2, r.openIssues());
    }

    @Test
    void contactsAreHiddenOnCancelledOrders() {
        Dto.OrderResponse r = Mappers.order(order(OrderStatus.CANCELLED), Role.ADMIN, List.of(), 0);
        assertNull(r.shopPhone());
        assertNull(r.farmerPhone());
        assertNull(r.deliveryCode());
        assertFalse(r.deliveryCodeRequired());
    }

    @Test
    void codeIsNoLongerRequiredAfterDelivery() {
        Dto.OrderResponse r = Mappers.order(order(OrderStatus.DELIVERED), Role.FARMER, List.of(), 0);
        assertFalse(r.deliveryCodeRequired());
    }

    @Test
    void orderStepsFollowTheRules() {
        assertTrue(OrderService.canTransition(OrderStatus.MATCHED, OrderStatus.CONFIRMED));
        assertTrue(OrderService.canTransition(OrderStatus.CONFIRMED, OrderStatus.DELIVERED));
        assertTrue(OrderService.canTransition(OrderStatus.DELIVERED, OrderStatus.COMPLETED));
        assertFalse(OrderService.canTransition(OrderStatus.MATCHED, OrderStatus.DELIVERED));
        assertFalse(OrderService.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        assertFalse(OrderService.canTransition(OrderStatus.FAILED, OrderStatus.CONFIRMED));
    }
}
