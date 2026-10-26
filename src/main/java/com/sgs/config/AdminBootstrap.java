package com.sgs.config;

import com.sgs.user.PasswordRules;
import com.sgs.user.Role;
import com.sgs.user.User;
import com.sgs.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Creates the very first admin from ADMIN_USERNAME / ADMIN_PASSWORD when the users table has no admin. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) return;
        AppProperties.Bootstrap b = props.bootstrap();
        if (b == null || b.adminUsername() == null || b.adminUsername().isBlank()
                || b.adminPassword() == null || b.adminPassword().length() < 10
                || PasswordRules.check(b.adminPassword()) != null) {
            log.warn("No admin exists. Set ADMIN_USERNAME and ADMIN_PASSWORD (min 10 chars) to create the first admin.");
            return;
        }
        User admin = new User();
        admin.setUsername(b.adminUsername().trim().toLowerCase(Locale.ROOT));
        admin.setPasswordHash(encoder.encode(b.adminPassword()));
        admin.setFullName(b.adminFullName() == null || b.adminFullName().isBlank() ? "Administrator" : b.adminFullName());
        admin.setRole(Role.ADMIN);
        admin.setMustChangePassword(true);
        users.save(admin);
        log.info("Created initial admin '{}'. You must change the password at first login; remove ADMIN_PASSWORD from .env.",
                admin.getUsername());
    }
}
