package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anlegedatum pro Rezept. Für den Bestand gibt es kein echtes Datum; er bekommt gestaffelte Zeitpunkte in
 * ID-Reihenfolge (je eine Minute Abstand, das jüngste Rezept zum Zeitpunkt der Migration), damit "Neueste zuerst"
 * dieselbe Reihenfolge ergibt wie bisher die IDs.
 */
public class V23__recipe_created_at extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V23__recipe_created_at.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        List<Long> ids = new ArrayList<>();
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE recipes ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL");
            try (ResultSet rs = st.executeQuery("SELECT id FROM recipes ORDER BY id")) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        }
        Map<Long, LocalDateTime> times = backfillTimes(ids, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        try (PreparedStatement update = c.prepareStatement("UPDATE recipes SET created_at = ? WHERE id = ?")) {
            for (Map.Entry<Long, LocalDateTime> e : times.entrySet()) {
                update.setTimestamp(1, Timestamp.valueOf(e.getValue()));
                update.setLong(2, e.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }
        log.info("Anlegedatum für {} Rezepte in ID-Reihenfolge nachgetragen", ids.size());
    }

    static Map<Long, LocalDateTime> backfillTimes(List<Long> idsAscending, LocalDateTime newest) {
        Map<Long, LocalDateTime> times = new LinkedHashMap<>();
        int last = idsAscending.size() - 1;
        for (int i = 0; i <= last; i++) {
            times.put(idsAscending.get(i), newest.minusMinutes(last - i));
        }
        return times;
    }
}
