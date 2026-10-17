package com.recipebook.controller;

import com.recipebook.dto.SignupRequest;
import com.recipebook.service.SignupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Offene Registrierung: beide Endpunkte ohne Anmeldung, IP-Limit wie beim Login (AuthRateLimitFilter). */
@RestController
@RequestMapping("/api/auth")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    // Immer 202 ohne Inhalt – egal, ob die Adresse neu ist oder schon ein Konto hat
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        signupService.signup(request.getVorname(), request.getNachname(), request.getEmail(), request.getPassword());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of("email", signupService.verify(request.get("token"))));
    }
}
