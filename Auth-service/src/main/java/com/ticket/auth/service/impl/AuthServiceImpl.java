package com.ticket.auth.service.impl;


import com.ticket.auth.dto.request.LoginRequest;
import com.ticket.auth.dto.request.RefreshTokenRequest;
import com.ticket.auth.dto.request.RegisterRequest;
import com.ticket.auth.dto.response.LoginResponse;
import com.ticket.auth.enums.Role;
import com.ticket.auth.repository.UserRepository;
import com.ticket.auth.security.JwtTokenProvider;
import com.ticket.auth.service.AuthService;
import com.ticket.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ModelMapper modelMapper;
    private final StringRedisTemplate redisTemplate;
    private final AuthenticationManager authenticationManager;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Spring Security sẽ tự động gọi UserDetailsService và PasswordEncoder để kiểm tra
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid username/email or password");
        }

        // Lấy thông tin user sau khi xác thực thành công
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        // Sinh cặp token JWT (Access Token & Refresh Token)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Sử dụng ModelMapper hoặc khởi tạo thủ công bằng Builder
        User user = modelMapper.map(request, User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setIsActive(true);

        userRepository.save(user);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cấp lại Access Token mới
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void logout(String token) {
        // Đưa token vào Redis Blacklist với thời gian sống tương ứng token còn lại (ví dụ 15 phút) để vô hiệu hóa
        if (jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            redisTemplate.opsForValue().set("blacklist:" + token, username, 15, TimeUnit.MINUTES);
        } else {
            throw new RuntimeException("Invalid token");
        }
    }
}
