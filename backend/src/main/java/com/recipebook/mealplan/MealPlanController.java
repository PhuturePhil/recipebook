package com.recipebook.mealplan;

import com.recipebook.model.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Essensplan der Familie (nur angemeldet, nur Mitglieder – sonst 403). Datumsangaben als yyyy-MM-dd. */
@RestController
@RequestMapping("/api/meal-plan")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @GetMapping
    public List<MealPlanService.EntryView> list(@RequestParam Long familyId, @RequestParam String from,
            @RequestParam String to, @AuthenticationPrincipal CustomUserDetails user) {
        return mealPlanService.list(user.getId(), familyId, from, to);
    }

    @PostMapping
    public ResponseEntity<MealPlanService.EntryView> create(@RequestBody MealPlanService.CreateRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanService.create(user.getId(), body));
    }

    public record MoveRequest(String date) {}

    @PatchMapping("/{id}")
    public MealPlanService.EntryView move(@PathVariable Long id, @RequestBody MoveRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return mealPlanService.move(user.getId(), id, body == null ? null : body.date());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        mealPlanService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
