package com.ticket.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Main {
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "123456";

        String hashedPassword = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hashedPassword);

        // Test verify
        boolean matches = encoder.matches(password, hashedPassword);

        System.out.println("Match: " + matches);
    }
}