package com.recipebook.nutrition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Schreibt eine Referenzquelle idempotent (Upsert über den Code) in nutrient_dataset/reference_food.
 * Wird von Flyway-Migrationen genutzt; eine neue BLS-Version = neue CSV + neue Migration mit derselben Klasse.
 */
public class ReferenceFoodImporter {

    private static final int BATCH_SIZE = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public int importDataset(Connection connection, ReferenceFoodSource source) throws SQLException {
        DatasetInfo info = source.dataset();
        List<ReferenceFoodRow> rows = source.rows();
        long datasetId = upsertDataset(connection, info, rows.size());
        String sql = """
            INSERT INTO reference_food (code, dataset_id, name_de, name_en, kcal, kj, water, protein, fat, carbs,
              fiber, sugar, salt, alcohol, saturated_fat, micronutrients)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
            ON CONFLICT (code) DO UPDATE SET dataset_id = EXCLUDED.dataset_id, name_de = EXCLUDED.name_de,
              name_en = EXCLUDED.name_en, kcal = EXCLUDED.kcal, kj = EXCLUDED.kj, water = EXCLUDED.water,
              protein = EXCLUDED.protein, fat = EXCLUDED.fat, carbs = EXCLUDED.carbs, fiber = EXCLUDED.fiber,
              sugar = EXCLUDED.sugar, salt = EXCLUDED.salt, alcohol = EXCLUDED.alcohol,
              saturated_fat = EXCLUDED.saturated_fat, micronutrients = EXCLUDED.micronutrients
            """;
        int count = 0;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (ReferenceFoodRow r : rows) {
                int i = 1;
                ps.setString(i++, r.code());
                ps.setLong(i++, datasetId);
                ps.setString(i++, r.nameDe());
                ps.setString(i++, r.nameEn());
                Double[] values = {r.kcal(), r.kj(), r.water(), r.protein(), r.fat(), r.carbs(), r.fiber(), r.sugar(),
                    r.salt(), r.alcohol(), r.saturatedFat()};
                for (Double v : values) {
                    if (v == null) ps.setNull(i++, Types.DOUBLE);
                    else ps.setDouble(i++, v);
                }
                ps.setString(i, json(r));
                ps.addBatch();
                if (++count % BATCH_SIZE == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private long upsertDataset(Connection connection, DatasetInfo info, int rowCount) throws SQLException {
        String sql = """
            INSERT INTO nutrient_dataset (source_key, name, version, publisher, license, license_url, citation,
              source_url, doi, row_count, imported_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (source_key) DO UPDATE SET name = EXCLUDED.name, version = EXCLUDED.version,
              publisher = EXCLUDED.publisher, license = EXCLUDED.license, license_url = EXCLUDED.license_url,
              citation = EXCLUDED.citation, source_url = EXCLUDED.source_url, doi = EXCLUDED.doi,
              row_count = EXCLUDED.row_count, imported_at = CURRENT_TIMESTAMP
            RETURNING id
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, info.sourceKey());
            ps.setString(2, info.name());
            ps.setString(3, info.version());
            ps.setString(4, info.publisher());
            ps.setString(5, info.license());
            ps.setString(6, info.licenseUrl());
            ps.setString(7, info.citation());
            ps.setString(8, info.sourceUrl());
            ps.setString(9, info.doi());
            ps.setInt(10, rowCount);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private String json(ReferenceFoodRow r) {
        try {
            return objectMapper.writeValueAsString(r.micronutrients());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
