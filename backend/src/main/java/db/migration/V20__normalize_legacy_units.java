package db.migration;

import com.recipebook.service.IngredientUnits;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bringt alte Einheiten deutscher Rezepte auf die Schreibweise, die das Speichern seit Paket 4 für neue Zeilen
 * verwendet (St./Stueck → Stück, Teel. → TL, Dosen → Dose …) – exakt über {@link IngredientUnits#normalize}.
 * Englische Rezepte bleiben unangetastet: ihre Einheiten gehören zum Original, die deutsche Fassung rechnet um.
 * Unbekanntes und Freitext ("P.", "Schuss", "kleine Dose") lässt normalize ohnehin stehen.
 */
public class V20__normalize_legacy_units extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V20__normalize_legacy_units.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        List<Object[]> changes = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT i.id, i.recipe_id, i.unit FROM ingredients i "
                 + "JOIN recipes r ON r.id = i.recipe_id WHERE r.language = 'de' AND i.unit IS NOT NULL AND i.unit <> '' "
                 + "ORDER BY i.recipe_id, i.id")) {
            while (rs.next()) {
                String unit = rs.getString(3);
                String normalized = IngredientUnits.normalize(unit);
                if (!normalized.equals(unit)) changes.add(new Object[] {rs.getLong(1), rs.getLong(2), unit, normalized});
            }
        }
        Map<String, Integer> summary = new TreeMap<>();
        try (PreparedStatement update = c.prepareStatement("UPDATE ingredients SET unit = ? WHERE id = ? AND unit = ?")) {
            for (Object[] ch : changes) {
                update.setString(1, (String) ch[3]);
                update.setLong(2, (Long) ch[0]);
                update.setString(3, (String) ch[2]);
                update.executeUpdate();
                summary.merge("'" + ch[2] + "' → '" + ch[3] + "'", 1, Integer::sum);
                log.info("Einheit vereinheitlicht: Rezept {}, Zeile {}: '{}' → '{}'", ch[1], ch[0], ch[2], ch[3]);
            }
        }
        log.info("Alt-Einheiten vereinheitlicht: {} Zeilen in deutschen Rezepten {}", changes.size(), summary);
    }
}
