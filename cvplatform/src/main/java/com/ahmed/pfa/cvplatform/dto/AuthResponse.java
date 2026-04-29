package com.ahmed.pfa.cvplatform.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String message;
    private Long userId;
    private String email;
    private String token;  // Nouveau champ pour JWT

    // Constructeur pour Register (sans token)
    public AuthResponse(String message, Long userId, String email) {
        this.message = message;
        this.userId = userId;
        this.email = email;
    }

    // Constructeur pour Login (avec token)
    public AuthResponse(String message, Long userId, String email, String token) {
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.token = token;
    }
}