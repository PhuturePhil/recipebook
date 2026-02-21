package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.PasswordResetTokenRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    
    @Value("${app.url:http://89.167.44.213}")
    private String appUrl;
    
    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }
    
    public record LoginResult(String token, CustomUserDetails userDetails) {}
    
    public LoginResult login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        
        UserDetails user = loadUserByUsername(email);
        String token = jwtService.generateToken(user);
        CustomUserDetails customUser = (CustomUserDetails) user;
        return new LoginResult(token, customUser);
    }
    
    public User register(String vorname, String nachname, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setVorname(vorname);
        user.setNachname(nachname);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        
        return userRepository.save(user);
    }
    
    public User createUser(String vorname, String nachname, String email, String password, Role role) {
        return register(vorname, nachname, email, password, role);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getId() != null 
                ? userRepository.findByEmail(((CustomUserDetails) authentication.getPrincipal()).getUsername()).orElse(null)
                : null;
        }
        return null;
    }
    
    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }
    
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        tokenRepository.deleteByUser(user);
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user, LocalDateTime.now().plusHours(1)
        );
        tokenRepository.save(resetToken);
        
        sendPasswordResetEmail(user.getEmail(), token);
    }
    
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        
        if (!resetToken.isValid()) {
            throw new RuntimeException("Token expired");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
    
    private void sendPasswordResetEmail(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Passwort zurücksetzen - RecipeBook");
            message.setText("Um Ihr Passwort zurückzusetzen, klicken Sie auf folgenden Link:\n\n"
                    + appUrl + "/reset-password?token=" + token + "\n\n"
                    + "Sie haben 1 Stunde Zeit, um Ihr Passwort zurückzusetzen.\n\n"
                    + "Ihr RecipeBook Team");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    private UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}
