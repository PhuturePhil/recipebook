package com.recipebook.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    
    private final Long id;
    private final String email;
    private final String password;
    private final String vorname;
    private final String nachname;
    private final Role role;
    private final boolean enabled;
    
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.vorname = user.getVorname();
        this.nachname = user.getNachname();
        this.role = user.getRole();
        this.enabled = true;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getVorname() {
        return vorname;
    }
    
    public String getNachname() {
        return nachname;
    }
    
    public Role getRole() {
        return role;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
