package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeCreatedAtMigrationTest {

    private Connection connection;
    private Context context;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:v23createdat;DB_CLOSE_DELAY=-1", "sa", "");
        context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE recipes (id BIGINT PRIMARY KEY, title VARCHAR(255))");
            st.execute("INSERT INTO recipes VALUES (12, 'Dal'), (3, 'Quiche'), (7, 'Shakshuka'), (40, 'Chili')");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        connection.close();
    }

    private List<Long> ids(String sql) throws Exception {
        List<Long> values = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getLong(1));
        }
        return values;
    }

    @Test
    void backfillIsStaggeredInIdOrderEndingAtTheNewest() {
        LocalDateTime newest = LocalDateTime.of(2026, 9, 27, 18, 0);
        Map<Long, LocalDateTime> times = V23__recipe_created_at.backfillTimes(List.of(3L, 7L, 12L), newest);

        assertEquals(List.of(3L, 7L, 12L), new ArrayList<>(times.keySet()));
        assertEquals(newest.minusMinutes(2), times.get(3L));
        assertEquals(newest.minusMinutes(1), times.get(7L));
        assertEquals(newest, times.get(12L));
        assertTrue(V23__recipe_created_at.backfillTimes(List.of(), newest).isEmpty());
    }

    @Test
    void newestFirstByCreatedAtMatchesDescendingIds() throws Exception {
        new V23__recipe_created_at().migrate(context);

        assertEquals(List.of(40L, 12L, 7L, 3L), ids("SELECT id FROM recipes ORDER BY created_at DESC"));
        assertEquals(4L, ids("SELECT COUNT(DISTINCT created_at) FROM recipes").get(0));
    }

    @Test
    void newRecipesGetTheCurrentTimeByDefault() throws Exception {
        new V23__recipe_created_at().migrate(context);
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO recipes (id, title) VALUES (41, 'Neu')");
            try (ResultSet rs = st.executeQuery("SELECT created_at FROM recipes WHERE id = 41")) {
                rs.next();
                assertNotNull(rs.getTimestamp(1));
            }
        }
        assertEquals(41L, ids("SELECT id FROM recipes ORDER BY created_at DESC, id DESC").get(0));
    }
}
