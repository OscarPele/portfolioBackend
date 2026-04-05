package com.portfolioBackend.config;

import com.portfolioBackend.auth.user.UserRepository;
import com.portfolioBackend.auth.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final UserService userService;

    @Value("${app.seed.username}")
    private String username;

    @Value("${app.seed.email}")
    private String email;

    @Value("${app.seed.password}")
    private String rawPassword;

    public DataSeeder(UserRepository userRepo, UserService userService) {
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(email))
                .ifPresentOrElse(existing -> {
                    if (!existing.isEnabled()) {
                        existing.setEnabled(true);
                        userRepo.save(existing);
                    }
                    userService.forceChangePassword(existing.getId(), rawPassword);
                }, () -> {
                    var u = userService.register(username, email, rawPassword);
                    u.setEnabled(true);
                    userRepo.save(u);
                });
    }
}
