package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Repeating requests, for example 50 kg of tomatoes every Monday. */
@Service
@RequiredArgsConstructor
public class StandingOrderService {
    private final StandingOrderRepository repo;
    private final DemandRequestRepository demands;
    private final CropRepository crops;
    private final ProfileResolver profiles;
    private final NotificationService notifier;

    public static LocalDate nextDate(LocalDate d, Frequency f) {
        return switch (f) {
            case WEEKLY -> d.plusWeeks(1);
            case EVERY_2_WEEKS -> d.plusWeeks(2);
            case MONTHLY -> d.plusMonths(1);
        };
    }

    private Dto.StandingOrderResponse map(StandingOrder s) {
        return new Dto.StandingOrderResponse(s.getId(), s.getCrop().getId(), s.getCrop().getName(), s.getCrop().getUnit(),
                s.getQuantity(), s.getMaxPrice(), s.getFrequency(), s.getNextNeededDate(), s.getLeadDays(), s.isActive(),
                s.getNotes(), s.getLastRunOn());
    }

    private StandingOrder own(AuthUser u, Long id) {
        ShopProfile shop = profiles.shop(u);
        return repo.findById(id).filter(x -> x.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> ApiException.notFound("Repeating request"));
    }

    @Transactional
    public Dto.StandingOrderResponse create(AuthUser u, Dto.StandingOrderRequest r) {
        Crop crop = crops.findById(r.cropId()).filter(Crop::isActive).orElseThrow(() -> ApiException.notFound("Crop"));
        StandingOrder s = new StandingOrder();
        s.setShop(profiles.shop(u)); s.setCrop(crop); s.setQuantity(r.quantity()); s.setMaxPrice(r.maxPrice());
        s.setFrequency(r.frequency()); s.setNextNeededDate(r.firstNeededDate());
        s.setLeadDays(r.leadDays() == null ? 2 : r.leadDays()); s.setNotes(r.notes());
        repo.save(s);
        runFor(s, LocalDate.now());   // if it is already due, send the first request now
        return map(s);
    }

    @Transactional(readOnly = true)
    public List<Dto.StandingOrderResponse> list(AuthUser u) {
        return repo.findByShopIdOrderByCreatedAtDesc(profiles.shop(u).getId()).stream().map(this::map).toList();
    }

    @Transactional
    public Dto.StandingOrderResponse setActive(AuthUser u, Long id, boolean active) {
        StandingOrder s = own(u, id);
        s.setActive(active);
        if (active) {
            // skip dates that already passed while it was paused
            int guard = 0;
            while (s.getNextNeededDate().isBefore(LocalDate.now()) && guard++ < 60) s.setNextNeededDate(nextDate(s.getNextNeededDate(), s.getFrequency()));
        }
        return map(s);
    }

    @Transactional
    public void delete(AuthUser u, Long id) { repo.delete(own(u, id)); }

    /** Called every morning. Returns how many requests were created. */
    @Transactional
    public int runDue(LocalDate today) {
        int n = 0;
        for (StandingOrder s : repo.findByActiveTrue()) if (runFor(s, today)) n++;
        return n;
    }

    private boolean runFor(StandingOrder s, LocalDate today) {
        boolean created = false;
        int guard = 0;
        while (!s.getNextNeededDate().minusDays(s.getLeadDays()).isAfter(today) && guard++ < 4) {
            LocalDate needed = s.getNextNeededDate();
            if (!needed.isBefore(today)) {
                DemandRequest d = new DemandRequest();
                d.setShop(s.getShop()); d.setCrop(s.getCrop()); d.setQuantity(s.getQuantity()); d.setMaxPrice(s.getMaxPrice());
                d.setNeededBy(needed); d.setNotes(s.getNotes()); d.setStatus(DemandStatus.OPEN);
                demands.save(d);
                created = true;
                Map<String, String> p = new HashMap<>();
                p.put("qty", s.getQuantity().stripTrailingZeros().toPlainString());
                p.put("unit", s.getCrop().getUnit());
                p.put("crop", s.getCrop().getName());
                p.put("date", needed.toString());
                notifier.send(s.getShop().getUser(), "STANDING_CREATED", p);
            }
            s.setNextNeededDate(nextDate(needed, s.getFrequency()));
            s.setLastRunOn(today);
        }
        return created;
    }
}
