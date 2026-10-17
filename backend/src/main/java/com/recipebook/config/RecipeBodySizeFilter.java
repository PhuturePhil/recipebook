package com.recipebook.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/** Begrenzt Rezept-Anlegen/-Speichern auf 4 MB (Foto höchstens 3 MB plus Text); nginx begrenzt zusätzlich. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RecipeBodySizeFilter extends OncePerRequestFilter {

    static final long MAX_BYTES = 4L * 1024 * 1024;
    static final String MESSAGE = "Das Rezept ist zu groß. Bitte verwende ein kleineres Foto.";
    private static final Pattern RECIPE_WRITE = Pattern.compile("^/api/recipes(/[0-9]+)?/?$");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        return !("POST".equals(method) || "PUT".equals(method)) || !RECIPE_WRITE.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":413,\"message\":\"" + MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
