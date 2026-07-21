package com.ticket.auth.service;


import com.ticket.auth.dto.request.LoginRequest;
import com.ticket.auth.dto.request.RefreshTokenRequest;
import com.ticket.auth.dto.request.RegisterRequest;
import com.ticket.auth.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void register(RegisterRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
}
