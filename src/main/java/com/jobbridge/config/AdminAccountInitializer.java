package com.jobbridge.config;

import com.jobbridge.model.PortalUser;
import com.jobbridge.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
public class AdminAccountInitializer {
    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String email,
            @Value("${ADMIN_PASSWORD:}") String password,
            @Value("${ADMIN_FULL_NAME:JobBridge Administrator}") String fullName) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.isBlank()) {
                throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD to provision an admin account.");
            }
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("ADMIN_PASSWORD must be at least 12 characters and at most 72 UTF-8 bytes.");
            }

            var existing = users.findByEmailIgnoreCase(email.trim());
            if (existing.isPresent()) {
                if (!"ADMIN".equals(existing.get().getRole())) {
                    throw new IllegalStateException("ADMIN_EMAIL is already used by a non-admin account.");
                }
                return;
            }

            PortalUser admin = new PortalUser();
            admin.setFullName(fullName.trim());
            admin.setEmail(email.trim());
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole("ADMIN");
            users.save(admin);
        };
    }
}
