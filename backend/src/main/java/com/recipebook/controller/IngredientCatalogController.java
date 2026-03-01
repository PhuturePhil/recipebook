package com.recipebook.controller;

import com.recipebook.dto.IngredientCatalogDto;
import com.recipebook.model.IngredientCatalog;
import com.recipebook.repository.IngredientCatalogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ingredient-catalog")
public class IngredientCatalogController {

    private final IngredientCatalogRepository repository;

    public IngredientCatalogController(IngredientCatalogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<IngredientCatalogDto>> getAll() {
        List<IngredientCatalogDto> result = repository.findAllByOrderByNameAscUnitAsc()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IngredientCatalogDto> create(@RequestBody IngredientCatalogDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name darf nicht leer sein.");
        }
        IngredientCatalog entry = new IngredientCatalog(
            dto.getName().trim(),
            dto.getUnit() != null ? dto.getUnit().trim() : "",
            dto.getNutritionKcal(),
            dto.getNutritionFat(),
            dto.getNutritionProtein(),
            dto.getNutritionCarbs(),
            dto.getNutritionFiber()
        );
        IngredientCatalog saved = repository.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IngredientCatalogDto> update(@PathVariable Long id, @RequestBody IngredientCatalogDto dto) {
        IngredientCatalog entry = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden."));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            entry.setName(dto.getName().trim());
        }
        if (dto.getUnit() != null) {
            entry.setUnit(dto.getUnit().trim());
        }
        entry.setNutritionKcal(dto.getNutritionKcal());
        entry.setNutritionFat(dto.getNutritionFat());
        entry.setNutritionProtein(dto.getNutritionProtein());
        entry.setNutritionCarbs(dto.getNutritionCarbs());
        entry.setNutritionFiber(dto.getNutritionFiber());
        return ResponseEntity.ok(toDto(repository.save(entry)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden.");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private IngredientCatalogDto toDto(IngredientCatalog e) {
        return new IngredientCatalogDto(
            e.getId(), e.getName(), e.getUnit(),
            e.getNutritionKcal(), e.getNutritionFat(),
            e.getNutritionProtein(), e.getNutritionCarbs(), e.getNutritionFiber()
        );
    }
}
