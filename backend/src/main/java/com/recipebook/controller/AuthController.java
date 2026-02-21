package com.recipebook.controller;

import com.recipebook.dto.*;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.User;
import com.recipebook.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        
        CustomUserDetails userDetails = authService.getCurrentUserDetails();
        UserResponse userResponse = new UserResponse(
                userDetails.getId(),
                userDetails.getVorname(),
                userDetails.getNachname(),
                userDetails.getUsername(),
                userDetails.getRole()
        );
        
        return ResponseEntity.ok(new LoginResponse(token, userResponse));
    }
    
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.createUser(
                request.getVorname(),
                request.getNachname(),
                request.getEmail(),
                request.getPassword(),
                request.getRole()
        );
        
        UserResponse response = new UserResponse(
                user.getId(),
                user.getVorname(),
                user.getNachname(),
                user.getEmail(),
                user.getRole()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = authService.getAllUsers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getVorname(),
                        user.getNachname(),
                        user.getEmail(),
                        user.getRole()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = new UserResponse(
                userDetails.getId(),
                userDetails.getVorname(),
                userDetails.getNachname(),
                userDetails.getUsername(),
                userDetails.getRole()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
    
    public static class LoginResponse {
        private String token;
        private UserResponse user;
        
        public LoginResponse(String token, UserResponse user) {
            this.token = token;
            this.user = user;
        }
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public UserResponse getUser() {
            return user;
        }
        
        public void setUser(UserResponse user) {
            this.user = user;
        }
    }
}
