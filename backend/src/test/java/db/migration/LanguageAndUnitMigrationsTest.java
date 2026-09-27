package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LanguageAndUnitMigrationsTest {

    private Connection connection;
    private Context context;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:p6migrations;DB_CLOSE_DELAY=-1", "sa", "");
        context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE recipes (id BIGINT PRIMARY KEY, title VARCHAR(255), description TEXT, "
                + "language VARCHAR(2) DEFAULT 'de' NOT NULL)");
            st.execute("CREATE TABLE ingredients (id BIGINT PRIMARY KEY, recipe_id BIGINT, name VARCHAR(255), "
                + "unit VARCHAR(255), sort_order INT)");
            st.execute("CREATE TABLE recipe_instructions (recipe_id BIGINT, step TEXT, sort_order INT)");
            st.execute("INSERT INTO recipes (id, title) VALUES (1, 'Porree-Quiche'), (2, 'Turkish green beans')");
            st.execute("INSERT INTO ingredients VALUES (10, 1, 'Zwiebeln', 'St', 0), (11, 1, 'Kreuzkümmel', 'Teel.', 1), "
                + "(12, 1, 'Kichererbsen', 'Dosen', 2), (13, 1, 'Vanillezucker', 'P.', 3), (14, 1, 'Salz', NULL, 4), "
                + "(15, 1, 'Mais', 'kleine Dose', 5), (16, 1, 'Mehl', 'g', 6), "
                + "(20, 2, 'runner beans', 'g', 0), (21, 2, 'finely chopped onion', 'pc', 1), "
                + "(22, 2, 'red wine vinegar', 'tablespoon', 2), (23, 2, 'olive oil', 'cup', 3)");
            st.execute("INSERT INTO recipe_instructions VALUES (1, 'Den Porree waschen und in Ringe schneiden.', 0), "
                + "(2, 'Heat the oil in a pan and cook the onion until soft.', 0), "
                + "(2, 'Add the beans and cook for 15 minutes.', 1)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        connection.close();
    }

    private List<String> column(String sql) throws Exception {
        List<String> values = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getString(1));
        }
        return values;
    }

    @Test
    void classifiesEnglishRecipesAndNormalizesOnlyGermanUnits() throws Exception {
        new V18__classify_recipe_language().migrate(context);
        assertEquals(List.of("de", "en"), column("SELECT language FROM recipes ORDER BY id"));

        new V20__normalize_legacy_units().migrate(context);
        assertEquals(java.util.Arrays.asList("Stück", "TL", "Dose", "P.", null, "kleine Dose", "g"),
            column("SELECT unit FROM ingredients WHERE recipe_id = 1 ORDER BY id"));
        assertEquals(List.of("g", "pc", "tablespoon", "cup"),
            column("SELECT unit FROM ingredients WHERE recipe_id = 2 ORDER BY id"));
    }
}
