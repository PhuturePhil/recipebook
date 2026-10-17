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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
}
