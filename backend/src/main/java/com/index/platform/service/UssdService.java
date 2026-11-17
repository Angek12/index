package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import com.index.platform.util.PhoneUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A simple USSD menu so farmers without smartphones can use Index (Africa's Talking style callback). */
@Service
@RequiredArgsConstructor
public class UssdService {
    private final UserRepository users;
    private final CropRepository crops;
    private final FarmSupplyRepository supplies;
    private final ProfileResolver profiles;
    private final OrderService orderService;
    private final ReportService reports;

    @Transactional
    public String handle(String phone, String text) {
        User u = users.findByPhoneIn(PhoneUtil.variants(phone)).stream()
                .filter(x -> x.getRole() == Role.FARMER && x.isActive() && x.getApprovalStatus() == ApprovalStatus.APPROVED)
                .findFirst().orElse(null);
        if (u == null) return "END This number is not registered as an Index farmer. Please sign up on the Index website.";
        boolean rw = "rw".equals(u.getLanguage());
        AuthUser au = new AuthUser(u.getId(), u.getEmail(), u.getRole());
        String[] p = (text == null || text.isBlank()) ? new String[0] : text.split("\\*", -1);
        try {
            return route(au, rw, p);
        } catch (ApiException e) {
            return "END " + e.getMessage();
        } catch (NumberFormatException e) {
            return "END " + (rw ? "Umubare si wo. Ongera ugerageze." : "That number is not valid. Please try again.");
        } catch (Exception e) {
            return "END " + (rw ? "Habaye ikosa. Ongera ugerageze." : "Something went wrong. Please try again.");
        }
    }

    private String route(AuthUser au, boolean rw, String[] p) {
        if (p.length == 0) {
            return "CON Index\n" + (rw
                    ? "1. Shyiraho umusaruro\n2. Ibyifuzwa ku isoko\n3. Komande zanjye\n4. Emeza komande"
                    : "1. List produce\n2. Market demand\n3. My orders\n4. Act on an order");
        }
        switch (p[0]) {
            case "1": return listProduce(au, rw, p);
            case "2": return market(rw);
            case "3": return myOrders(au, rw);
            case "4": return act(au, rw, p);
            default: return "END " + (rw ? "Amahitamo ntabwo ari yo." : "Invalid choice.");
        }
    }

    private String listProduce(AuthUser au, boolean rw, String[] p) {
        List<Crop> cs = crops.findByActiveTrueOrderByName().stream().limit(8).toList();
        if (p.length == 1) {
            StringBuilder sb = new StringBuilder("CON " + (rw ? "Hitamo igihingwa:" : "Choose crop:"));
            for (int i = 0; i < cs.size(); i++) sb.append("\n").append(i + 1).append(". ").append(cs.get(i).getName());
            return sb.toString();
        }
        int idx = Integer.parseInt(p[1].trim()) - 1;
        if (idx < 0 || idx >= cs.size()) return "END " + (rw ? "Amahitamo ntabwo ari yo." : "Invalid choice.");
        Crop crop = cs.get(idx);
        if (p.length == 2) return "CON " + (rw ? "Ingano (" + crop.getUnit() + "):" : "Quantity (" + crop.getUnit() + "):");
        if (p.length == 3) return "CON " + (rw ? "Igiciro kuri " + crop.getUnit() + " (RWF):" : "Price per " + crop.getUnit() + " (RWF):");
        BigDecimal qty = new BigDecimal(p[2].trim());
        BigDecimal price = new BigDecimal(p[3].trim());
        if (qty.signum() <= 0 || price.signum() < 0) throw ApiException.badRequest(rw ? "Umubare si wo." : "Invalid quantity or price.");
        FarmSupply s = new FarmSupply();
        s.setFarmer(profiles.farmer(au)); s.setCrop(crop); s.setQuantityAvailable(qty); s.setPricePerUnit(price);
        s.setAvailableFrom(LocalDate.now()); s.setStatus(SupplyStatus.AVAILABLE); s.setNotes("Added by USSD");
        supplies.save(s);
        return "END " + (rw ? "Byashyizweho: " : "Listed: ") + qty.stripTrailingZeros().toPlainString() + " " + crop.getUnit()
                + " " + crop.getName() + " @ " + price.stripTrailingZeros().toPlainString() + " RWF";
    }

    private String market(boolean rw) {
        List<Dto.MarketDemand> list = reports.marketDemand().stream().limit(5).toList();
        if (list.isEmpty()) return "END " + (rw ? "Nta cyifuzo gihari ubu." : "No open requests right now.");
        StringBuilder sb = new StringBuilder("END " + (rw ? "Ibyifuzwa:" : "Wanted now:"));
        for (Dto.MarketDemand m : list) {
            sb.append("\n").append(m.cropName()).append(" ").append(m.totalQuantity().stripTrailingZeros().toPlainString()).append(" ").append(m.unit());
        }
        return sb.toString();
    }

    private String myOrders(AuthUser au, boolean rw) {
        List<Dto.OrderResponse> list = orderService.list(au, null).stream().limit(5).toList();
        if (list.isEmpty()) return "END " + (rw ? "Nta komande ufite." : "You have no orders yet.");
        StringBuilder sb = new StringBuilder("END");
        for (Dto.OrderResponse o : list) {
            sb.append("\n#").append(o.id()).append(" ").append(o.cropName()).append(" ")
                    .append(o.quantity().stripTrailingZeros().toPlainString()).append(o.unit()).append(" ").append(o.status());
        }
        return sb.toString();
    }

    private String act(AuthUser au, boolean rw, String[] p) {
        List<Dto.OrderResponse> todo = orderService.list(au, null).stream()
                .filter(o -> o.status() == OrderStatus.MATCHED || o.status() == OrderStatus.CONFIRMED).limit(5).toList();
        if (todo.isEmpty()) return "END " + (rw ? "Nta komande isaba igikorwa." : "No orders need action.");
        if (p.length == 1) {
            StringBuilder sb = new StringBuilder("CON " + (rw ? "Hitamo komande:" : "Choose order:"));
            for (int i = 0; i < todo.size(); i++) {
                Dto.OrderResponse o = todo.get(i);
                boolean confirm = o.status() == OrderStatus.MATCHED;
                sb.append("\n").append(i + 1).append(". #").append(o.id()).append(" ").append(o.cropName()).append(" - ")
                        .append(confirm ? (rw ? "Emeza" : "Confirm") : (rw ? "Yagejejwe" : "Delivered"));
            }
            return sb.toString();
        }
        int idx = Integer.parseInt(p[1].trim()) - 1;
        if (idx < 0 || idx >= todo.size()) return "END " + (rw ? "Amahitamo ntabwo ari yo." : "Invalid choice.");
        Dto.OrderResponse o = todo.get(idx);
        OrderStatus target = o.status() == OrderStatus.MATCHED ? OrderStatus.CONFIRMED : OrderStatus.DELIVERED;
        boolean needsCode = target == OrderStatus.DELIVERED && o.deliveryCodeRequired();
        if (needsCode && p.length == 2) return "CON " + (rw ? "Andika kode y'iduka y'imibare 4:" : "Enter the shop's 4-digit delivery code:");
        String code = needsCode ? p[2].trim() : null;
        orderService.updateStatus(au, o.id(), new Dto.OrderStatusRequest(target, "Via USSD", null, null, code));
        return "END " + (rw ? "Komande #" : "Order #") + o.id() + " " + (target == OrderStatus.CONFIRMED ? (rw ? "yemejwe." : "confirmed.") : (rw ? "yagejejwe." : "marked delivered."));
    }
}
