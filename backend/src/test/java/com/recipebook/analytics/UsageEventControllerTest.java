package com.recipebook.analytics;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsageEventController.class, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UsageEventControllerTest {

    private static final String BODY = """
            {"events":[{"event":"rezept_geoeffnet","ts":1760695200000,"sessionId":"abc","device":"mobile",
            "display":"pwa","props":{"recipeId":99,"herkunft":"suche"}}]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsageEventService usageEventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static CustomUserDetails principal(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.de");
        u.setPassword("x");
        u.setRole(Role.USER);
        return new CustomUserDetails(u);
    }

    @Test
    void withoutAuth_isRejected() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usageEventService);
    }

    @Test
    void withAuth_passesBatchWithUserId() throws Exception {
        mockMvc.perform(post("/api/events").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UsageEventBatch> captor = ArgumentCaptor.forClass(UsageEventBatch.class);
        verify(usageEventService).record(eq(5L), captor.capture());
        UsageEventBatch.Event e = captor.getValue().events().get(0);
        assertEquals("rezept_geoeffnet", e.event());
        assertEquals(1760695200000L, e.ts());
        assertEquals("suche", e.props().get("herkunft"));
        assertEquals(99, ((Number) e.props().get("recipeId")).intValue());
    }

    @Test
    void emptyEventList_isAccepted() throws Exception {
        mockMvc.perform(post("/api/events").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON).content("{\"events\":[]}"))
                .andExpect(status().isNoContent());
        verify(usageEventService).record(eq(5L), any());
    }

    @Test
    void tooManyRequests_isRateLimited() throws Exception {
        for (int i = 0; i < UsageEventController.MAX_REQUESTS_PER_MINUTE; i++) {
            mockMvc.perform(post("/api/events").with(user(principal(9))).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(post("/api/events").with(user(principal(9))).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests());
        verify(usageEventService, times(UsageEventController.MAX_REQUESTS_PER_MINUTE)).record(eq(9L), any());
    }

    @Test
    void malformedBody_is400() throws Exception {
        mockMvc.perform(post("/api/events").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON).content("{kaputt"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usageEventService);
    }

    private static final String PUBLIC_BODY = """
            {"events":[{"event":"teilen_link_aufgerufen","ts":1760695200000,"sessionId":"abc","device":"mobile",
            "display":"browser","props":{"token":"abc123DEF456ghi789JKLm"}}]}""";

    @Test
    void publicEndpoint_worksWithoutLoginAndPassesBatch() throws Exception {
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.1")
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UsageEventBatch> captor = ArgumentCaptor.forClass(UsageEventBatch.class);
        verify(usageEventService).recordAnonymous(captor.capture());
        UsageEventBatch.Event e = captor.getValue().events().get(0);
        assertEquals("teilen_link_aufgerufen", e.event());
        assertEquals("abc123DEF456ghi789JKLm", e.props().get("token"));
        verify(usageEventService, never()).record(anyLong(), any());
    }

    @Test
    void publicEndpoint_withLogin_staysAnonymous() throws Exception {
        mockMvc.perform(post("/api/events/public").with(user(principal(5))).header("X-Forwarded-For", "203.0.113.2")
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isNoContent());
        verify(usageEventService).recordAnonymous(any());
        verify(usageEventService, never()).record(anyLong(), any());
    }

    @Test
    void publicEndpoint_onlyPost() throws Exception {
        mockMvc.perform(get("/api/events/public")).andExpect(status().isForbidden());
        verifyNoInteractions(usageEventService);
    }

    @Test
    void publicEndpoint_rejectsLargeBody() throws Exception {
        String big = "{\"events\":[{\"event\":\"teilen_link_aufgerufen\",\"props\":{\"x\":\""
                + "a".repeat(UsageEventController.MAX_PUBLIC_BODY_BYTES) + "\"}}]}";
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.3")
                        .contentType(MediaType.APPLICATION_JSON).content(big))
                .andExpect(status().is(413));
        verifyNoInteractions(usageEventService);
    }

    @Test
    void publicEndpoint_malformedBody_is400() throws Exception {
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.4")
                        .contentType(MediaType.APPLICATION_JSON).content("{kaputt"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usageEventService);
    }

    @Test
    void publicEndpoint_isRateLimitedPerClient() throws Exception {
        for (int i = 0; i < UsageEventController.MAX_PUBLIC_REQUESTS_PER_MINUTE_PER_CLIENT; i++) {
            mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.5")
                            .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.5")
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "203.0.113.6")
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isNoContent());
        verify(usageEventService, times(UsageEventController.MAX_PUBLIC_REQUESTS_PER_MINUTE_PER_CLIENT + 1))
                .recordAnonymous(any());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void publicEndpoint_hasGlobalLimit() throws Exception {
        for (int i = 0; i < UsageEventController.MAX_PUBLIC_REQUESTS_PER_MINUTE; i++) {
            mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "198.51.100." + (i % 250) + "-" + i)
                    .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY));
        }
        mockMvc.perform(post("/api/events/public").header("X-Forwarded-For", "192.0.2.99")
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isTooManyRequests());
    }
}
