package com.jauharxlr.springbase.auth.service;

import com.jauharxlr.springbase.auth.dto.AuthResponse;
import com.jauharxlr.springbase.auth.dto.LoginRequest;
import com.jauharxlr.springbase.auth.dto.SignupRequest;
import com.jauharxlr.springbase.auth.entity.User;
import com.jauharxlr.springbase.auth.repository.UserRepository;
import com.jauharxlr.springbase.common.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByEmailAndProjectRef(request.getEmail(), request.getProjectRef()).isPresent()) {
            throw new RuntimeException("Email already registered in this project");
        }

        java.util.UUID companyId = request.getCompanyId();
        java.util.UUID tenantId = request.getTenantId();

        // Extract from options.data if available (Supabase compatibility)
        if (request.getOptions() != null && request.getOptions().getData() != null) {
            java.util.Map<String, Object> data = request.getOptions().getData();
            if (companyId == null && data.containsKey("company_id")) {
                companyId = java.util.UUID.fromString(data.get("company_id").toString());
            }
            if (tenantId == null && data.containsKey("tenant_id")) {
                tenantId = java.util.UUID.fromString(data.get("tenant_id").toString());
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .projectRef(request.getProjectRef())
                .role("authenticated")
                .companyId(companyId)
                .tenantId(tenantId)
                .build();
        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getId(), user.getProjectRef(), user.getRole(), user.getCompanyId(), user.getTenantId());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndProjectRef(request.getEmail(), request.getProjectRef())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getProjectRef(), user.getRole(), user.getCompanyId(), user.getTenantId());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }
}
