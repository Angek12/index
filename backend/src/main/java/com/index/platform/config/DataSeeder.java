package com.index.platform.config;

import com.index.platform.domain.Role;
import com.index.platform.domain.User;
import com.index.platform.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup if it does not exist. Change the password after first login. */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email, password, fullName;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.admin.email}") String email,
                      @Value("${app.admin.password}") String password,
                      @Value("${app.admin.full-name}") String fullName) {
        this.users = users; this.encoder = encoder;
        this.email = email.toLowerCase(); this.password = password; this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(email)) return;
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(password));
        admin.setFullName(fullName);
        admin.setRole(Role.ADMIN);
        users.save(admin);
        log.info("Seeded admin account {}", email);
    }
}
