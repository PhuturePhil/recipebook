package com.recipebook.controller;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.AiQuotaService;
import com.recipebook.service.RecipeScanService;
import com.recipebook.testutil.TestImages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeScanControllerTest {

    @Mock private RecipeScanService scanService;

    private int used;
    private RecipeScanController controller;

    @BeforeEach
    void setUp() {
        AiQuotaService quota = new AiQuotaService(new AiQuotaService.UsageStore() {
            public boolean tryIncrement(String key, LocalDate day, String kind, int limit) {
                if (used >= limit) return false;
                used++;
                return true;
            }
            public void deleteBefore(LocalDate day) { }
        }, Clock.systemUTC());
        controller = new RecipeScanController(scanService, quota);
        login(Role.USER);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Role role) {
        User u = new User();
        u.setId(7L);
        u.setEmail("anna@test.de");
        u.setRole(role);
        CustomUserDetails d = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities()));
    }

    private static Map<String, String> photo(String mimeType, byte[] bytes) {
        return Map.of("imageData", Base64.getEncoder().encodeToString(bytes), "mimeType", mimeType);
    }

    private static List<Map<String, String>> photos(int n) {
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(photo("image/jpeg", TestImages.jpeg()));
        return list;
    }

    @Test
    void atMostFivePhotosPerScan() throws Exception {
        assertEquals(200, controller.scanRecipe(photos(5)).getStatusCode().value());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.scanRecipe(photos(6)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("höchstens 5"));
        verify(scanService, times(1)).scanImages(any());
    }

    @Test
    void tenScansPerDay_thenTooManyRequests() throws Exception {
        for (int i = 0; i < 10; i++) controller.scanRecipe(photos(1));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.scanRecipe(photos(1)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertEquals("Du hast heute schon 10 Rezepte per Foto erfasst – morgen geht es weiter.", ex.getReason());
        verify(scanService, times(10)).scanImages(any());
    }

    @Test
    void adminsAreNotLimited() throws Exception {
        login(Role.ADMIN);
        for (int i = 0; i < 12; i++) controller.scanRecipe(photos(1));
        verify(scanService, times(12)).scanImages(any());
        assertEquals(0, used);
    }

    @Test
    void contentIsCheckedBeforeAnythingIsCounted() throws Exception {
        assertThrows(ResponseStatusException.class,
            () -> controller.scanRecipe(List.of(photo("image/jpeg", "<svg/>".getBytes()))));
        assertThrows(ResponseStatusException.class,
            () -> controller.scanRecipe(List.of(photo("image/svg+xml", TestImages.jpeg()))));
        assertThrows(ResponseStatusException.class,
            () -> controller.scanRecipe(List.of(photo("image/png", TestImages.pngHeader(20_000, 20_000)))));
        assertThrows(ResponseStatusException.class,
            () -> controller.scanRecipe(List.of(Map.of("imageData", "%%%nicht-base64", "mimeType", "image/jpeg"))));
        assertEquals(0, used, "abgelehnte Uploads kosten kein Kontingent");
        verifyNoInteractions(scanService);
    }
}
