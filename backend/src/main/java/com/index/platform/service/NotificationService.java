package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.repo.*;
import com.index.platform.util.PhoneUtil;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** In-app notifications plus optional SMS and WhatsApp, in the user's own language (English or Kinyarwanda). */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final MessageGateway gateway;

    private static final Map<String, String[]> T = new HashMap<>();
    static {
        T.put("ORDER_MATCHED", new String[]{
                "New order #{id}: {qty} {unit} of {crop} between {shop} and {farmer}. Open Index to confirm.",
                "Komande nshya #{id}: {qty} {unit} za {crop} hagati ya {shop} na {farmer}. Fungura Index urebe."});
        T.put("ORDER_CONFIRMED", new String[]{
                "Order #{id} ({crop}) was confirmed by the farmer.",
                "Komande #{id} ({crop}) yemejwe n'umuhinzi."});
        T.put("ORDER_DELIVERED", new String[]{
                "Order #{id} ({crop}) was delivered. Please confirm you received it.",
                "Komande #{id} ({crop}) yagejejwe. Nyamuneka wemeze ko wayakiriye."});
        T.put("ORDER_COMPLETED", new String[]{
                "Order #{id} ({crop}) is complete. Thank you.",
                "Komande #{id} ({crop}) yarangiye. Murakoze."});
        T.put("ORDER_FAILED", new String[]{
                "Order #{id} ({crop}) failed. Reason: {note}",
                "Komande #{id} ({crop}) ntiyagenze neza. Impamvu: {note}"});
        T.put("ORDER_CANCELLED", new String[]{
                "Order #{id} ({crop}) was cancelled. Reason: {note}",
                "Komande #{id} ({crop}) yahagaritswe. Impamvu: {note}"});
        T.put("PAYMENT_PAID", new String[]{
                "The shop marked order #{id} as paid ({method}). Please confirm when you receive the money.",
                "Iduka ryavuze ko ryishyuye komande #{id} ({method}). Emeza igihe wakiriye amafaranga."});
        T.put("PAYMENT_RECEIVED", new String[]{
                "The farmer confirmed receiving payment for order #{id}.",
                "Umuhinzi yemeje ko yakiriye amafaranga ya komande #{id}."});
        T.put("ACCOUNT_APPROVED", new String[]{
                "Your Index account is approved. You can now log in.",
                "Konti yawe ya Index yemejwe. Ubu ushobora kwinjira."});
        T.put("ACCOUNT_REJECTED", new String[]{
                "Your Index account request was not approved. Please contact the Index team.",
                "Gusaba konti ya Index ntibyemejwe. Nyamuneka vugana n'itsinda rya Index."});
        T.put("NEW_ACCOUNT_PENDING", new String[]{
                "New {role} account waiting for approval: {name}.",
                "Konti nshya ({role}) itegereje kwemezwa: {name}."});
        T.put("ORDER_CODE", new String[]{
                "Your delivery code for order #{id} ({crop}) is {code}. Give it to the farmer only when you receive the goods.",
                "Kode yawe yo kwemeza ko komande #{id} ({crop}) yagezweho ni {code}. Yihe umuhinzi gusa igihe wakiriye ibicuruzwa."});
        T.put("ISSUE_REPORTED", new String[]{
                "A problem was reported on order #{id} ({crop}): {type}.",
                "Hatanzwe ikibazo kuri komande #{id} ({crop}): {type_rw}."});
        T.put("ISSUE_RESOLVED", new String[]{
                "The problem on order #{id} ({crop}) was resolved. {note}",
                "Ikibazo kuri komande #{id} ({crop}) cyarakemuwe. {note}"});
        T.put("ISSUE_DISMISSED", new String[]{
                "The problem report on order #{id} ({crop}) was closed without action. {note}",
                "Ikibazo cyatanzwe kuri komande #{id} ({crop}) cyafunzwe nta gikorwa. {note}"});
        T.put("PHONE_CODE", new String[]{
                "Your Index verification code is {code}. It expires in 30 minutes.",
                "Kode yawe yo kwemeza telefoni ya Index ni {code}. Irarangira mu minota 30."});
        T.put("STANDING_CREATED", new String[]{
                "Your repeating request for {qty} {unit} of {crop} was sent for {date}.",
                "Icyifuzo cyawe gihoraho cya {qty} {unit} za {crop} cyoherejwe ku itariki {date}."});
        T.put("ATTENTION_DIGEST", new String[]{
                "Index needs attention: {text}",
                "Index ikeneye ko ubikurikirana: {text_rw}"});
        T.put("SUPPORT_NEW", new String[]{
                "New support message from {name}: {subject}",
                "Ubutumwa bushya bw'ubufasha buvuye kuri {name}: {subject}"});
        T.put("SUPPORT_REPLY", new String[]{
                "The Index team replied to your message: {subject}",
                "Itsinda rya Index ryasubije ubutumwa bwawe: {subject}"});
        T.put("SYSTEM_ERROR", new String[]{
                "System error: {text}",
                "Ikosa rya sisitemu: {text}"});
        T.put("PASSWORD_RESET_CODE", new String[]{
                "Your Index password reset code is {code}. It expires in 30 minutes.",
                "Kode yawe yo guhindura ijambobanga rya Index ni {code}. Irarangira mu minota 30."});
    }

    private static String render(String key, String lang, Map<String, String> p) {
        String[] t = T.getOrDefault(key, new String[]{key, key});
        String s = "rw".equals(lang) ? t[1] : t[0];
        for (Map.Entry<String, String> e : p.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return s;
    }

    private static String linkFor(String key) {
        if (key.startsWith("ORDER_") || key.startsWith("PAYMENT_")) return "/orders";
        if (key.equals("NEW_ACCOUNT_PENDING")) return "/users";
        if (key.startsWith("ISSUE_")) return "/orders";
        if (key.startsWith("SUPPORT_")) return "/support";
        if (key.equals("ATTENTION_DIGEST")) return "/dashboard";
        return null;
    }

    private void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { r.run(); }
            });
        } else {
            r.run();
        }
    }

    /** Saves an in-app notification and sends SMS or WhatsApp if the user allows it. */
    @Transactional
    public void send(User u, String key, Map<String, String> params) {
        String msg = render(key, u.getLanguage(), params);
        Notification n = new Notification();
        n.setUser(u); n.setMessage(msg); n.setLink(linkFor(key));
        notifications.save(n);
        String phone = PhoneUtil.normalize(u.getPhone());
        if (phone == null) return;
        if (u.isNotifySms()) afterCommit(() -> gateway.sendSms(phone, "Index: " + msg));
        if (u.isNotifyWhatsapp()) afterCommit(() -> gateway.sendWhatsapp(phone, "Index: " + msg));
    }

    /** SMS only, no in-app record. Used for password reset codes. */
    public void sendSmsOnly(User u, String key, Map<String, String> params) {
        String phone = PhoneUtil.normalize(u.getPhone());
        if (phone == null) return;
        String msg = render(key, u.getLanguage(), params);
        afterCommit(() -> gateway.sendSms(phone, "Index: " + msg));
    }

    @Transactional
    public void notifyAdmins(String key, Map<String, String> params) {
        for (User a : users.findByRoleOrderByCreatedAtDesc(Role.ADMIN)) send(a, key, params);
    }

    @Transactional(readOnly = true)
    public List<Dto.NotificationResponse> latest(Long userId) {
        return notifications.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new Dto.NotificationResponse(n.getId(), n.getMessage(), n.getLink(), n.isSeen(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) { return notifications.countByUserIdAndSeenFalse(userId); }

    @Transactional
    public void markAllRead(Long userId) { notifications.markAllSeen(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        notifications.findById(id).filter(n -> n.getUser().getId().equals(userId)).ifPresent(n -> n.setSeen(true));
    }
}
