package com.recipebook.nutrition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SeedFiles {

    public static final String INGREDIENTS = "nutrition/ingredient-seed.csv";
    public static final String CONVERSIONS = "nutrition/unit-conversions.csv";

    private SeedFiles() {
    }

    public static List<SeedIngredient> ingredients(InputStream in) {
        List<SeedIngredient> result = new ArrayList<>();
        for (String[] f : rows(in)) {
            List<String> aliases = f.length > 4 && !f[4].isBlank()
                ? Arrays.stream(f[4].split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
            result.add(new SeedIngredient(f[0].trim(), IngredientClass.parse(f[1]),
                f.length > 2 && !f[2].isBlank() ? f[2].trim() : null,
                f.length > 3 && Boolean.parseBoolean(f[3].trim()), aliases));
        }
        return result;
    }

    public static List<SeedConversion> conversions(InputStream in) {
        List<SeedConversion> result = new ArrayList<>();
        for (String[] f : rows(in)) {
            IngredientClass cls = f[1].isBlank() ? null : IngredientClass.parse(f[1]);
            String ingredient = f[2].isBlank() ? null : f[2].trim();
            String note = f.length > 4 && !f[4].isBlank() ? f[4].trim() : null;
            result.add(new SeedConversion(UnitNormalizer.canonical(f[0]), cls, ingredient,
                Double.parseDouble(f[3].trim()), note));
        }
        return result;
    }

    private static List<String[]> rows(InputStream in) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                rows.add(line.split(";", -1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }
}
