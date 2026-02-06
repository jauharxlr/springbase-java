package com.jauharxlr.springbase.config;

import com.jauharxlr.springbase.auth.entity.User;
import com.jauharxlr.springbase.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.findByEmail("admin@springbase.io").isEmpty()) {
                userRepository.save(User.builder()
                        .email("admin@springbase.io")
                        .password(passwordEncoder.encode("admin123"))
                        .projectRef("default")
                        .role("service_role")
                        .build());
            }
        };
    }
}
