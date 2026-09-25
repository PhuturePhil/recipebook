package db.migration;

import com.recipebook.nutrition.CatalogMigrationPlanner;
import com.recipebook.nutrition.NutritionCatalogWriter;
import com.recipebook.nutrition.SeedFiles;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;

public class V12__migrate_ingredient_catalog extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V12__migrate_ingredient_catalog.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        NutritionCatalogWriter writer = new NutritionCatalogWriter();
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream seeds = cl.getResourceAsStream(SeedFiles.INGREDIENTS);
             InputStream conversions = cl.getResourceAsStream(SeedFiles.CONVERSIONS)) {
            if (seeds == null || conversions == null) throw new IllegalStateException("Nährwert-Seed-Dateien fehlen");
            CatalogMigrationPlanner.Plan plan = CatalogMigrationPlanner.plan(
                SeedFiles.ingredients(seeds), SeedFiles.conversions(conversions),
                writer.readLegacy(c), writer.readReferenceCodes(c));
            writer.write(c, plan);
            log.info("Zutatenkatalog migriert: {} Altzeilen -> {} Zutaten ({} über Startzuordnung, {} automatisch gruppiert)",
                plan.legacyRows(), plan.ingredients().size(), plan.legacyMatchedToSeed(), plan.legacyAutoGrouped());
            plan.warnings().forEach(w -> log.warn("Katalog-Migration: {}", w));
        }
    }
}
