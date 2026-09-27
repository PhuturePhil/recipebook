package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeSourceUrlMigrationTest {

    private static final String DHANSAK = "Mild gewürzt und weich — auch für kleine Kinder geeignet, Röstzwiebeln dann "
        + "weglassen oder fein zerdrücken. Quelle: https://www.zeit.de/zeit-magazin/wochenmarkt/2025-11/kuerbis-linsen-dhansak-rezept-wochenmarkt";
    private static final String BLECH = "Gewürzmischung frei tauschbar (geräucherter Paprika, indische Mischung). Quelle: "
        + "ZEIT Magazin Wochenmarkt / Rukmini Iyer (Guardian) — https://www.zeit.de/zeit-magazin/wochenmarkt/2025-06/suesskartoffeln";
    private static final String CURRY = "Passt zu Reis, Naan oder als Füllung für Tortillas.\n\n"
        + "Quelle: https://biancazapatka.com/de/beluga-linsen-curry-indisches-dal-makhani/";
    private static final String FOCACCIA = "Classic Italian flatbread.\n\nToppings:\n- Onion: scatter extra thyme on top.\n\n"
        + "Source: https://www.jamieoliver.com/recipes/bread/focaccia/";

    private Connection connection;
    private Context context;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:v25sourceurl;DB_CLOSE_DELAY=-1", "sa", "");
        context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE recipes (id BIGINT PRIMARY KEY, title VARCHAR(255), description TEXT)");
        }
        insert(93, DHANSAK);
        insert(98, BLECH);
        insert(149, CURRY);
        insert(151, FOCACCIA);
        insert(3, "Die Anleitung steht auf https://example.com/anleitung und ist ausführlich.");
        insert(4, null);
        insert(5, "Ohne Link.");
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        connection.close();
    }

    private void insert(long id, String description) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO recipes VALUES (?, 'Rezept', ?)")) {
            ps.setLong(1, id);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    private String[] row(long id) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("SELECT source_url, description FROM recipes WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new String[] {rs.getString(1), rs.getString(2)};
            }
        }
    }

    @Test
    void labelInTheSameSentenceGoesWithTheLink() {
        V25__recipe_source_url.Moved moved = V25__recipe_source_url.split(DHANSAK);
        assertEquals("https://www.zeit.de/zeit-magazin/wochenmarkt/2025-11/kuerbis-linsen-dhansak-rezept-wochenmarkt", moved.url());
        assertEquals("Mild gewürzt und weich — auch für kleine Kinder geeignet, Röstzwiebeln dann weglassen oder fein zerdrücken.",
            moved.description());
    }

    @Test
    void sourceNoteBeforeTheLinkIsRemovedWithIt() {
        V25__recipe_source_url.Moved moved = V25__recipe_source_url.split(BLECH);
        assertEquals("https://www.zeit.de/zeit-magazin/wochenmarkt/2025-06/suesskartoffeln", moved.url());
        assertEquals("Gewürzmischung frei tauschbar (geräucherter Paprika, indische Mischung).", moved.description());
    }

    @Test
    void linkOnItsOwnLineTakesTheBlankLinesWithIt() {
        V25__recipe_source_url.Moved curry = V25__recipe_source_url.split(CURRY);
        assertEquals("https://biancazapatka.com/de/beluga-linsen-curry-indisches-dal-makhani/", curry.url());
        assertEquals("Passt zu Reis, Naan oder als Füllung für Tortillas.", curry.description());

        V25__recipe_source_url.Moved focaccia = V25__recipe_source_url.split(FOCACCIA);
        assertEquals("https://www.jamieoliver.com/recipes/bread/focaccia/", focaccia.url());
        assertEquals("Classic Italian flatbread.\n\nToppings:\n- Onion: scatter extra thyme on top.", focaccia.description());
    }

    @Test
    void bareLinkAtTheEndMovesWithoutTrailingPunctuation() {
        V25__recipe_source_url.Moved moved = V25__recipe_source_url.split("Sehr lecker – https://example.com/rezept.");
        assertEquals("https://example.com/rezept", moved.url());
        assertEquals("Sehr lecker", moved.description());

        V25__recipe_source_url.Moved only = V25__recipe_source_url.split("Quelle: https://example.com/rezept");
        assertEquals("https://example.com/rezept", only.url());
        assertNull(only.description());
    }

    @Test
    void linksInsideTheTextOrInvalidLinksStay() {
        assertNull(V25__recipe_source_url.split("Siehe https://example.com/a und dann weiter."));
        assertNull(V25__recipe_source_url.split("Quelle: https://localhost/x"));
        assertNull(V25__recipe_source_url.split("Ohne Link."));
        assertNull(V25__recipe_source_url.split(null));
    }

    @Test
    void migrationMovesTrailingLinksAndLeavesTheRest() throws Exception {
        new V25__recipe_source_url().migrate(context);

        assertEquals("https://www.zeit.de/zeit-magazin/wochenmarkt/2025-11/kuerbis-linsen-dhansak-rezept-wochenmarkt", row(93)[0]);
        assertEquals("Gewürzmischung frei tauschbar (geräucherter Paprika, indische Mischung).", row(98)[1]);
        assertEquals("Passt zu Reis, Naan oder als Füllung für Tortillas.", row(149)[1]);
        assertEquals("https://www.jamieoliver.com/recipes/bread/focaccia/", row(151)[0]);

        assertNull(row(3)[0]);
        assertEquals("Die Anleitung steht auf https://example.com/anleitung und ist ausführlich.", row(3)[1]);
        assertNull(row(4)[0]);
        assertNull(row(4)[1]);
        assertEquals("Ohne Link.", row(5)[1]);
    }
}
