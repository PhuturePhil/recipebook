package com.recipebook.family;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {FamilyController.class, FamilyInviteController.class}, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FamilyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyService familyService;

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

    private static FamilyService.FamilyView view(long id) {
        return new FamilyService.FamilyView(id, "Kochclub", 1, 10, 0,
            List.of(new FamilyService.Member(5L, "Paco Pastoors", "PP", true)), null);
    }

    @Test
    void familyManagement_needsLogin() throws Exception {
        mockMvc.perform(get("/api/families")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/families").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/families/3")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/families/3/members/5")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/family-invites/abcdefghijklmnopqrstuvwx/join")).andExpect(status().isForbidden());
        verifyNoInteractions(familyService);
    }

    @Test
    void create_returns201WithTheFamily() throws Exception {
        when(familyService.create(5L, "Kochclub")).thenReturn(view(3));

        mockMvc.perform(post("/api/families").with(user(principal(5)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Kochclub\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kochclub"))
                .andExpect(jsonPath("$.maxMembers").value(10))
                .andExpect(jsonPath("$.members[0].self").value(true));
    }

    @Test
    void foreignFamily_is404() throws Exception {
        when(familyService.get(3L, 6L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Die Familie wurde nicht gefunden."));

        mockMvc.perform(get("/api/families/3").with(user(principal(6))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Die Familie wurde nicht gefunden."));
    }

    @Test
    void leavingAsLastMember_reportsDeletion() throws Exception {
        when(familyService.removeMember(3L, 5L, 5L)).thenReturn(true);

        mockMvc.perform(delete("/api/families/3/members/5").with(user(principal(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void inviteLookup_isPublicNoStoreAndNoindex() throws Exception {
        when(familyService.inviteInfo("abcdefghijklmnopqrstuvwx", null))
                .thenReturn(new FamilyService.InviteInfo("Kochclub", "Paco", 3, 10, false, false, null));

        mockMvc.perform(get("/api/family-invites/abcdefghijklmnopqrstuvwx"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.familyName").value("Kochclub"))
                .andExpect(jsonPath("$.familyId").doesNotExist());
    }

    @Test
    void join_whenFull_is409() throws Exception {
        when(familyService.join("abcdefghijklmnopqrstuvwx", 5L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, FamilyService.FAMILY_FULL));

        mockMvc.perform(post("/api/family-invites/abcdefghijklmnopqrstuvwx/join").with(user(principal(5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(FamilyService.FAMILY_FULL));
    }
}
