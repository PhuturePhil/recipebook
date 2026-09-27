package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Link zur Originalseite als eigenes Feld. Bestand: Steht am Ende der Beschreibung ein Link (oft als „Quelle: …“ oder
 * „Source: …“), wandert er ins neue Feld und wird samt Hinweis aus der Beschreibung entfernt; der übrige Text bleibt.
 */
public class V25__recipe_source_url extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V25__recipe_source_url.class);

    static final int MAX_URL = 2048;

    private static final Pattern TRAILING_URL = Pattern.compile("(https?://\\S+?)[.,;:!)\\]]*\\s*\\z");
    private static final Pattern WEB_ADDRESS =
        Pattern.compile("(?i)https?://[^\\s/?#@:]+\\.[^\\s/?#@:]+(:\\d{1,5})?([/?#]\\S*)?");
    private static final Pattern LABEL = Pattern.compile("(?i)\\b(?:Quelle|Source)\\s*:");
    private static final Pattern TRAILING_SEPARATORS = Pattern.compile("[\\s—–\\-|:]+\\z");

    record Moved(String url, String description) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        Map<Long, Moved> moves = new LinkedHashMap<>();
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE recipes ADD COLUMN source_url VARCHAR(" + MAX_URL + ")");
            try (ResultSet rs = st.executeQuery("SELECT id, description FROM recipes WHERE description LIKE '%http%'")) {
                while (rs.next()) {
                    Moved moved = split(rs.getString(2));
                    if (moved != null) moves.put(rs.getLong(1), moved);
                }
            }
        }
        try (PreparedStatement update =
                 c.prepareStatement("UPDATE recipes SET source_url = ?, description = ? WHERE id = ?")) {
            for (Map.Entry<Long, Moved> e : moves.entrySet()) {
                update.setString(1, e.getValue().url());
                update.setString(2, e.getValue().description());
                update.setLong(3, e.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }
        log.info("Link zur Originalseite bei {} Rezepten aus der Beschreibung übernommen: {}", moves.size(), moves.keySet());
    }

    /** Link am Ende der Beschreibung und die Beschreibung ohne ihn, oder null wenn dort kein gültiger Link steht. */
    static Moved split(String description) {
        if (description == null) return null;
        Matcher m = TRAILING_URL.matcher(description);
        if (!m.find()) return null;
        String url = m.group(1);
        if (url.length() > MAX_URL || !WEB_ADDRESS.matcher(url).matches()) return null;

        String before = description.substring(0, m.start(1));
        // Ein „Quelle:“ in derselben Zeile gehört zum Link und fällt mit ihm weg
        Matcher label = LABEL.matcher(before);
        int cut = label.find(before.lastIndexOf('\n') + 1) ? label.start() : before.length();
        String rest = TRAILING_SEPARATORS.matcher(before.substring(0, cut)).replaceAll("");
        return new Moved(url, rest.isEmpty() ? null : rest);
    }
}
