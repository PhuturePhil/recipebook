package com.recipebook.family;

import com.recipebook.model.CustomUserDetails;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Einladungslinks: Nachschlagen ist öffentlich (Einladungsseite), Beitreten nur angemeldet. Beides läuft durch das
 * IP-Rate-Limit (AuthRateLimitFilter); ungültige und abgelaufene Codes liefern dieselbe 404-Meldung.
 */
@RestController
@RequestMapping("/api/family-invites")
public class FamilyInviteController {

    private final FamilyService familyService;

    public FamilyInviteController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<FamilyService.InviteInfo> lookup(@PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("X-Robots-Tag", "noindex, nofollow")
            .body(familyService.inviteInfo(code, user != null ? user.getId() : null));
    }

    @PostMapping("/{code}/join")
    public FamilyService.FamilyView join(@PathVariable String code, @AuthenticationPrincipal CustomUserDetails user) {
        return familyService.join(code, user.getId());
    }
}
