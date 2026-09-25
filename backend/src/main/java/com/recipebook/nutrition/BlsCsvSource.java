package com.recipebook.nutrition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

/**
 * Liest die aus der offiziellen BLS-Excel-Datei erzeugte CSV (siehe tools/bls/convert_bls.py).
 */
public class BlsCsvSource implements ReferenceFoodSource {

    public static final String RESOURCE = "nutrition/bls_4_0.csv.gz";

    public static final DatasetInfo BLS_4_0 = new DatasetInfo(
        "BLS",
        "Bundeslebensmittelschlüssel (BLS) – Deutsche Nährstoffdatenbank",
        "4.0",
        "Max Rubner-Institut",
        "CC BY 4.0",
        "https://creativecommons.org/licenses/by/4.0/deed.de",
        "Max Rubner-Institut (2025): Bundeslebensmittelschlüssel (BLS), Version 4.0 — Deutsche Nährstoffdatenbank. "
            + "Karlsruhe. DOI: 10.25826/Data20251217-134202-0",
        "https://blsdb.de",
        "10.25826/Data20251217-134202-0"
    );

    private static final List<String> MACRO_COLUMNS = List.of(
        "kcal", "kj", "water", "protein", "fat", "carbs", "fiber", "sugar", "salt", "alcohol", "saturated_fat");

    private final DatasetInfo dataset;
    private final Supplier<InputStream> gzipStream;

    public BlsCsvSource(DatasetInfo dataset, Supplier<InputStream> gzipStream) {
        this.dataset = dataset;
        this.gzipStream = gzipStream;
    }

    public static BlsCsvSource fromClasspath() {
        return new BlsCsvSource(BLS_4_0, () -> {
            InputStream in = BlsCsvSource.class.getClassLoader().getResourceAsStream(RESOURCE);
            if (in == null) throw new IllegalStateException("BLS-Datei fehlt im Classpath: " + RESOURCE);
            return in;
        });
    }

    @Override
    public DatasetInfo dataset() {
        return dataset;
    }

    @Override
    public List<ReferenceFoodRow> rows() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new GZIPInputStream(gzipStream.get()), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return List.of();
            String[] columns = header.split(";", -1);
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < columns.length; i++) index.put(columns[i], i);
            for (String required : List.of("code", "name_de", "kcal")) {
                if (!index.containsKey(required)) throw new IllegalStateException("BLS-CSV ohne Spalte " + required);
            }
            List<ReferenceFoodRow> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(row(parseCsvLine(line), index));
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ReferenceFoodRow row(List<String> f, Map<String, Integer> index) {
        Map<String, Double> micros = new LinkedHashMap<>();
        for (Micronutrients.Nutrient n : Micronutrients.ALL) {
            Double v = number(f, index.get(n.code()));
            if (v != null) micros.put(n.code(), v);
        }
        Map<String, Double> m = new HashMap<>();
        for (String c : MACRO_COLUMNS) m.put(c, number(f, index.get(c)));
        return new ReferenceFoodRow(
            f.get(index.get("code")).trim(),
            f.get(index.get("name_de")).trim(),
            index.containsKey("name_en") ? f.get(index.get("name_en")).trim() : null,
            m.get("kcal"), m.get("kj"), m.get("water"), m.get("protein"), m.get("fat"), m.get("carbs"),
            m.get("fiber"), m.get("sugar"), m.get("salt"), m.get("alcohol"), m.get("saturated_fat"), micros);
    }

    private static Double number(List<String> f, Integer i) {
        if (i == null || i >= f.size()) return null;
        String v = f.get(i).trim();
        if (v.isEmpty()) return null;
        return Double.parseDouble(v);
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ';') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
