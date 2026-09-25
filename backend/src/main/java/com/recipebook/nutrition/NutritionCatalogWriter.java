package com.recipebook.nutrition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Liest den Altkatalog und schreibt einen {@link CatalogMigrationPlanner.Plan} per JDBC (Flyway-Migration).
 */
public class NutritionCatalogWriter {

    public List<LegacyCatalogEntry> readLegacy(Connection c) throws SQLException {
        List<LegacyCatalogEntry> rows = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
            "SELECT id, name, unit, nutrition_kcal, nutrition_fat, nutrition_protein, nutrition_carbs, nutrition_fiber "
                + "FROM ingredient_catalog ORDER BY id")) {
            while (rs.next()) {
                rows.add(new LegacyCatalogEntry(rs.getLong(1), rs.getString(2), rs.getString(3), dbl(rs, 4), dbl(rs, 5),
                    dbl(rs, 6), dbl(rs, 7), dbl(rs, 8)));
            }
        }
        return rows;
    }

    public Set<String> readReferenceCodes(Connection c) throws SQLException {
        Set<String> codes = new HashSet<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT code FROM reference_food")) {
            while (rs.next()) codes.add(rs.getString(1));
        }
        return codes;
    }

    public Map<String, Long> write(Connection c, CatalogMigrationPlanner.Plan plan) throws SQLException {
        Map<String, Long> idByKey = new HashMap<>();
        String insertIngredient = """
            INSERT INTO nutrition_ingredient (name, name_key, ingredient_class, source, reference_code, negligible,
              kcal, protein, fat, carbs, fiber, sugar, salt, note)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (PreparedStatement ps = c.prepareStatement(insertIngredient)) {
            for (CatalogMigrationPlanner.PlannedIngredient p : plan.ingredients()) {
                ps.setString(1, p.name());
                ps.setString(2, p.key());
                ps.setString(3, p.ingredientClass().name());
                ps.setString(4, p.source().name());
                ps.setString(5, p.referenceCode());
                ps.setBoolean(6, p.negligible());
                NutrientProfile v = p.per100g();
                setDouble(ps, 7, v == null ? null : v.kcal());
                setDouble(ps, 8, v == null ? null : v.protein());
                setDouble(ps, 9, v == null ? null : v.fat());
                setDouble(ps, 10, v == null ? null : v.carbs());
                setDouble(ps, 11, v == null ? null : v.fiber());
                setDouble(ps, 12, v == null ? null : v.sugar());
                setDouble(ps, 13, v == null ? null : v.salt());
                ps.setString(14, p.note());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    idByKey.put(p.key(), rs.getLong(1));
                }
            }
        }
        try (PreparedStatement alias = c.prepareStatement(
            "INSERT INTO nutrition_ingredient_alias (alias_key, alias, ingredient_id, origin) VALUES (?, ?, ?, 'MIGRATION') "
                + "ON CONFLICT (alias_key) DO NOTHING");
             PreparedStatement legacy = c.prepareStatement(
                 "INSERT INTO nutrition_ingredient_legacy_link (legacy_catalog_id, ingredient_id, per100_basis) VALUES (?, ?, ?)")) {
            for (CatalogMigrationPlanner.PlannedIngredient p : plan.ingredients()) {
                Long id = idByKey.get(p.key());
                for (String a : p.aliases()) {
                    String key = IngredientNameNormalizer.key(a);
                    if (key.isEmpty() || key.equals(p.key())) continue;
                    alias.setString(1, key);
                    alias.setString(2, a);
                    alias.setLong(3, id);
                    alias.addBatch();
                }
                for (Long legacyId : p.legacyIds()) {
                    legacy.setLong(1, legacyId);
                    legacy.setLong(2, id);
                    legacy.setString(3, p.legacyBasis().get(legacyId));
                    legacy.addBatch();
                }
            }
            alias.executeBatch();
            legacy.executeBatch();
        }
        try (PreparedStatement conv = c.prepareStatement(
            "INSERT INTO unit_conversion (unit, ingredient_class, ingredient_id, grams, source, note) VALUES (?, ?, ?, ?, 'MANUAL', ?)")) {
            for (SeedConversion sc : plan.conversions()) {
                Long ingredientId = null;
                if (sc.ingredientName() != null) {
                    ingredientId = idByKey.get(IngredientNameNormalizer.canonicalKey(sc.ingredientName()));
                    if (ingredientId == null) continue;
                }
                conv.setString(1, sc.unit());
                conv.setString(2, sc.ingredientClass() == null ? null : sc.ingredientClass().name());
                if (ingredientId == null) conv.setNull(3, Types.BIGINT);
                else conv.setLong(3, ingredientId);
                conv.setDouble(4, sc.grams());
                conv.setString(5, sc.note());
                conv.addBatch();
            }
            conv.executeBatch();
        }
        return idByKey;
    }

    private static Double dbl(ResultSet rs, int i) throws SQLException {
        double v = rs.getDouble(i);
        return rs.wasNull() ? null : v;
    }

    private static void setDouble(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DOUBLE);
        else ps.setDouble(i, v);
    }
}
