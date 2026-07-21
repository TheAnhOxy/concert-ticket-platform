package com.ticket.auth.service.impl;


import com.ticket.auth.repository.UserRepository;
import com.ticket.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate; // Dùng để lưu blacklist khi logout hoặc quản lý refresh token

    @Override
    public LoginResponse login(LoginRequest request) {
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        // Sinh token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
        user.setRole(Role.ROLE_CUSTOMER); // Mặc định đăng ký là Customer
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
