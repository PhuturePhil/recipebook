package com.recipebook.config;

import com.recipebook.model.User;
import com.recipebook.service.OidcLoginService;
import com.recipebook.service.OidcLoginService.OidcIdentity;
import com.recipebook.service.OidcLoginService.OidcLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OidcAuthenticationHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthenticationHandler.class);

    private final OidcLoginService oidcLoginService;

    @Value("${app.url}")
    private String appUrl;

    public OidcAuthenticationHandler(OidcLoginService oidcLoginService) {
        this.oidcLoginService = oidcLoginService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        endSession(request);
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            redirectWithError(response, "invalid_principal");
            return;
        }
        try {
            User user = oidcLoginService.resolveUser(new OidcIdentity(
                    oidcUser.getSubject(),
                    oidcUser.getEmail(),
                    oidcUser.getEmailVerified(),
                    oidcUser.getGivenName(),
                    oidcUser.getFamilyName(),
                    oidcUser.getFullName()));
            String ticket = oidcLoginService.issueTicket(user);
            response.sendRedirect(UriComponentsBuilder.fromUriString(appUrl)
                    .path("/login/oidc")
                    .queryParam("ticket", ticket)
                    .toUriString());
        } catch (OidcLoginException e) {
            log.warn("OIDC-Login abgelehnt ({}): {}", e.getReason(), e.getMessage());
            redirectWithError(response, e.getReason());
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        endSession(request);
        String reason = exception instanceof OAuth2AuthenticationException oauth
                ? oauth.getError().getErrorCode()
                : "login_failed";
        log.warn("OIDC-Login fehlgeschlagen ({}): {}", reason, exception.getMessage());
        redirectWithError(response, reason);
    }

    private void redirectWithError(HttpServletResponse response, String reason) throws IOException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(appUrl)
                .path("/login")
                .queryParam("oidcError", reason)
                .toUriString());
    }

    private void endSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
