package com.index.platform.web;

import com.index.platform.service.UssdService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Callback for a USSD provider such as Africa's Talking. Set USSD_SECRET and use /api/ussd/{secret}. */
@RestController
@RequestMapping("/api/ussd")
public class UssdController {
    private final UssdService ussd;
    private final String secret;

    public UssdController(UssdService ussd, @Value("${app.ussd.secret:}") String secret) {
        this.ussd = ussd;
        this.secret = secret;
    }

    private ResponseEntity<String> reply(String phone, String text) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(ussd.handle(phone, text));
    }

    @PostMapping
    public ResponseEntity<String> open(@RequestParam(defaultValue = "") String phoneNumber,
                                       @RequestParam(defaultValue = "") String text) {
        if (!secret.isBlank()) return ResponseEntity.notFound().build();
        return reply(phoneNumber, text);
    }

    @PostMapping("/{given}")
    public ResponseEntity<String> secured(@PathVariable String given,
                                          @RequestParam(defaultValue = "") String phoneNumber,
                                          @RequestParam(defaultValue = "") String text) {
        boolean ok = !secret.isBlank() && MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
        if (!ok) return ResponseEntity.notFound().build();
        return reply(phoneNumber, text);
    }
}
