package db.migration;

import com.recipebook.translation.RecipeLanguage;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordnet den Rezeptbestand einmalig einer Sprache zu (dieselbe Erkennung wie beim Speichern). Alle Rezepte stehen
 * nach V17 auf "de"; hier werden nur die als Englisch erkannten umgestellt.
 */
public class V18__classify_recipe_language extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V18__classify_recipe_language.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        Map<Long, String[]> texts = new LinkedHashMap<>();
        Map<Long, List<String>> ingredients = new HashMap<>();
        Map<Long, List<String>> steps = new HashMap<>();
        try (Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT id, title, description FROM recipes ORDER BY id")) {
                while (rs.next()) texts.put(rs.getLong(1), new String[] {rs.getString(2), rs.getString(3)});
            }
            try (ResultSet rs = st.executeQuery("SELECT recipe_id, name FROM ingredients ORDER BY recipe_id, sort_order")) {
                while (rs.next()) ingredients.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2));
            }
            try (ResultSet rs = st.executeQuery("SELECT recipe_id, step FROM recipe_instructions ORDER BY recipe_id, sort_order")) {
                while (rs.next()) steps.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2));
            }
        }
        List<String> english = new ArrayList<>();
        try (PreparedStatement update = c.prepareStatement("UPDATE recipes SET language = 'en' WHERE id = ?")) {
            for (Map.Entry<Long, String[]> e : texts.entrySet()) {
                Long id = e.getKey();
                String language = RecipeLanguage.detect(e.getValue()[0], e.getValue()[1],
                    ingredients.getOrDefault(id, List.of()), steps.getOrDefault(id, List.of()));
                if (RecipeLanguage.ENGLISH.equals(language)) {
                    update.setLong(1, id);
                    update.executeUpdate();
                    english.add(id + " " + e.getValue()[0]);
                }
            }
        }
        log.info("Sprache zugeordnet: {} Rezepte, davon {} Englisch: {}", texts.size(), english.size(), english);
    }
}
