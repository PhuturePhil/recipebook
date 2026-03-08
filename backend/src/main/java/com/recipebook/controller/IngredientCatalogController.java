package com.recipebook.controller;

import com.recipebook.dto.IngredientCatalogDto;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.NutritionIngredientUnit;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.NutritionIngredientUnitRepository;
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

  private final NutritionIngredientUnitRepository unitRepository;
  private final NutritionIngredientRepository ingredientRepository;

  public IngredientCatalogController(
    NutritionIngredientUnitRepository unitRepository,
    NutritionIngredientRepository ingredientRepository
  ) {
    this.unitRepository = unitRepository;
    this.ingredientRepository = ingredientRepository;
  }

  @GetMapping
  public ResponseEntity<List<IngredientCatalogDto>> getAll() {
    List<IngredientCatalogDto> result = unitRepository.findAllByOrderByNameDeAscUnitDescriptionAsc()
      .stream()
      .map(this::toDto)
      .collect(Collectors.toList());
    return ResponseEntity.ok(result);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<IngredientCatalogDto> create(@RequestBody IngredientCatalogDto dto) {
    if (dto.getNameDe() == null || dto.getNameDe().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deutscher Name darf nicht leer sein.");
    }
    if (dto.getNameEn() == null || dto.getNameEn().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Englischer Name darf nicht leer sein.");
    }
    if (dto.getUnitDescription() == null || dto.getUnitDescription().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Einheit darf nicht leer sein.");
    }
    if (dto.getGramsPerUnit() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gramm pro Einheit darf nicht leer sein.");
    }

    NutritionIngredient ingredient = ingredientRepository.findByNameEn(dto.getNameEn().trim())
      .orElseGet(() -> {
        NutritionIngredient ni = new NutritionIngredient();
        ni.setNameEn(dto.getNameEn().trim());
        ni.setCalories100g(dto.getCalories100g());
        ni.setFat100g(dto.getFat100g());
        ni.setProtein100g(dto.getProtein100g());
        ni.setCarbs100g(dto.getCarbs100g());
        ni.setFiber100g(dto.getFiber100g());
        ni.setEstimated(Boolean.TRUE.equals(dto.getIsEstimated()));
        return ingredientRepository.save(ni);
      });

    NutritionIngredientUnit unit = new NutritionIngredientUnit();
    unit.setIngredient(ingredient);
    unit.setNameDe(dto.getNameDe().trim());
    unit.setUnitDescription(dto.getUnitDescription().trim());
    unit.setGramsPerUnit(dto.getGramsPerUnit());
    NutritionIngredientUnit saved = unitRepository.save(unit);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
  }

  @PutMapping("/unit/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<IngredientCatalogDto> updateUnit(@PathVariable Long id, @RequestBody IngredientCatalogDto dto) {
    NutritionIngredientUnit unit = unitRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Einheit nicht gefunden."));
    if (dto.getNameDe() != null && !dto.getNameDe().isBlank()) unit.setNameDe(dto.getNameDe().trim());
    if (dto.getUnitDescription() != null && !dto.getUnitDescription().isBlank()) unit.setUnitDescription(dto.getUnitDescription().trim());
    if (dto.getGramsPerUnit() != null) unit.setGramsPerUnit(dto.getGramsPerUnit());
    return ResponseEntity.ok(toDto(unitRepository.save(unit)));
  }

  @PutMapping("/ingredient/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<IngredientCatalogDto> updateIngredient(@PathVariable Long id, @RequestBody IngredientCatalogDto dto) {
    NutritionIngredient ingredient = ingredientRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zutat nicht gefunden."));
    if (dto.getNameEn() != null && !dto.getNameEn().isBlank()) ingredient.setNameEn(dto.getNameEn().trim());
    ingredient.setCalories100g(dto.getCalories100g());
    ingredient.setFat100g(dto.getFat100g());
    ingredient.setProtein100g(dto.getProtein100g());
    ingredient.setCarbs100g(dto.getCarbs100g());
    ingredient.setFiber100g(dto.getFiber100g());
    if (dto.getIsEstimated() != null) ingredient.setEstimated(dto.getIsEstimated());
    ingredientRepository.save(ingredient);
    List<NutritionIngredientUnit> units = unitRepository.findAllByOrderByNameDeAscUnitDescriptionAsc()
      .stream()
      .filter(u -> u.getIngredient().getId().equals(id))
      .toList();
    if (!units.isEmpty()) return ResponseEntity.ok(toDto(units.get(0)));
    return ResponseEntity.ok(new IngredientCatalogDto());
  }

  @DeleteMapping("/unit/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
    if (!unitRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Einheit nicht gefunden.");
    }
    unitRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private IngredientCatalogDto toDto(NutritionIngredientUnit unit) {
    NutritionIngredient ingredient = unit.getIngredient();
    IngredientCatalogDto dto = new IngredientCatalogDto();
    dto.setUnitId(unit.getId());
    dto.setIngredientId(ingredient.getId());
    dto.setNameDe(unit.getNameDe());
    dto.setNameEn(ingredient.getNameEn());
    dto.setUnitDescription(unit.getUnitDescription());
    dto.setGramsPerUnit(unit.getGramsPerUnit());
    dto.setCalories100g(ingredient.getCalories100g());
    dto.setFat100g(ingredient.getFat100g());
    dto.setProtein100g(ingredient.getProtein100g());
    dto.setCarbs100g(ingredient.getCarbs100g());
    dto.setFiber100g(ingredient.getFiber100g());
    dto.setIsEstimated(ingredient.isEstimated());
    return dto;
  }
}
