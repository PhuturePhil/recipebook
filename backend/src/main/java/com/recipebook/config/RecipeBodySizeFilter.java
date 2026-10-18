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

/**
 * Begrenzt Rezept-Anlegen/-Speichern auf 4 MB (Foto höchstens 3 MB plus Text) und Foto-Scans auf 8 MB
 * (höchstens 5 verkleinerte Fotos); nginx begrenzt zusätzlich.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RecipeBodySizeFilter extends OncePerRequestFilter {

    static final long MAX_BYTES = 4L * 1024 * 1024;
    static final String MESSAGE = "Das Rezept ist zu groß. Bitte verwende ein kleineres Foto.";
    static final long MAX_SCAN_BYTES = 8L * 1024 * 1024;
    static final String SCAN_MESSAGE = "Die Fotos sind zusammen zu groß (höchstens 8 MB). Bitte nimm weniger Fotos.";
    private static final Pattern RECIPE_WRITE = Pattern.compile("^/api/recipes(/[0-9]+)?/?$");
    private static final Pattern SCAN = Pattern.compile("^/api/recipes/scan/?$");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("POST".equals(method) && SCAN.matcher(uri).matches()) return false;
        return !("POST".equals(method) || "PUT".equals(method)) || !RECIPE_WRITE.matcher(uri).matches();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean scan = SCAN.matcher(request.getRequestURI()).matches();
        if (request.getContentLengthLong() > (scan ? MAX_SCAN_BYTES : MAX_BYTES)) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":413,\"message\":\"" + (scan ? SCAN_MESSAGE : MESSAGE) + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
