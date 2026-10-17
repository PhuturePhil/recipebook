package com.recipebook.family;

import com.recipebook.model.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Familien-Verwaltung (nur angemeldet); jede Aktion setzt die Mitgliedschaft voraus (sonst 404). */
@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping
    public List<FamilyService.FamilyView> myFamilies(@AuthenticationPrincipal CustomUserDetails user) {
        return familyService.myFamilies(user.getId());
    }

    // shareOwnRecipes fehlt (ältere App-Stände) → Familie startet wie bisher ohne Rezepte
    public record CreateRequest(String name, Boolean shareOwnRecipes) {}

    @PostMapping
    public ResponseEntity<FamilyService.FamilyView> create(@RequestBody CreateRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            familyService.create(user.getId(), body.name(), Boolean.TRUE.equals(body.shareOwnRecipes())));
    }

    @GetMapping("/{id}")
    public FamilyService.FamilyView get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return familyService.get(id, user.getId());
    }

    @PutMapping("/{id}")
    public FamilyService.FamilyView rename(@PathVariable Long id, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return familyService.rename(id, user.getId(), body.get("name"));
    }

    @PostMapping("/{id}/invite")
    public FamilyService.FamilyView renewInvite(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return familyService.renewInvite(id, user.getId());
    }

    // Eigene ID = austreten; als letztes Mitglied verschwindet die Familie (deleted: true)
    @DeleteMapping("/{id}/members/{userId}")
    public Map<String, Boolean> removeMember(@PathVariable Long id, @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("deleted", familyService.removeMember(id, user.getId(), userId));
    }
}
