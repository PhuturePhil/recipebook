package com.recipebook.controller;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.AuthService;
import com.recipebook.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {
    
    private final RecipeService recipeService;
    private final AuthService authService;
    
    public RecipeController(RecipeService recipeService, AuthService authService) {
        this.recipeService = recipeService;
        this.authService = authService;
    }
    
    @GetMapping
    public List<Recipe> getAllRecipes() {
        return recipeService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id) {
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public List<Recipe> searchRecipes(@RequestParam String q) {
        return recipeService.search(q);
    }
    
    @PostMapping("/test-create")
    public Recipe testCreate(@RequestBody Recipe recipe) {
        System.out.println("=== RECEIVED RECIPE ===");
        System.out.println("title: " + recipe.getTitle());
        System.out.println("author: " + recipe.getAuthor());
        System.out.println("source: " + recipe.getSource());
        System.out.println("page: " + recipe.getPage());
        System.out.println("======================");
        return recipe;
    }
    
    @PostMapping("/test-json")
    public String testJson(@RequestBody String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Recipe recipe = mapper.readValue(json, Recipe.class);
        System.out.println("=== PARSED RECIPE ===");
        System.out.println("title: " + recipe.getTitle());
        System.out.println("author: " + recipe.getAuthor());
        System.out.println("source: " + recipe.getSource());
        System.out.println("page: " + recipe.getPage());
        System.out.println("======================");
        return mapper.writeValueAsString(recipe);
    }
    
    @GetMapping("/test")
    public String testRecipe() {
        Recipe r = new Recipe();
        r.setTitle("Test");
        r.setAuthor("Test Author");
        r.setSource("Test Source");
        r.setPage("10");
        return "title=" + r.getTitle() + ", author=" + r.getAuthor() + ", source=" + r.getSource() + ", page=" + r.getPage();
    }
    
    @PostMapping
    public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = authService.getCurrentUserDetails().getId() != null 
            ? authService.getCurrentUser() 
            : null;
        Recipe saved = recipeService.save(recipe, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!recipeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        boolean isOwner = recipeService.isOwner(id, authService.getCurrentUser());
        
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        recipe.setId(id);
        Recipe updated = recipeService.save(recipe, authService.getCurrentUser());
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!recipeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        boolean isOwner = recipeService.isOwner(id, authService.getCurrentUser());
        
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        recipeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
