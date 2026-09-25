package db.migration;

import com.recipebook.nutrition.BlsCsvSource;
import com.recipebook.nutrition.ReferenceFoodImporter;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V11__import_bls_4_0 extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V11__import_bls_4_0.class);

    @Override
    public void migrate(Context context) throws Exception {
        int count = new ReferenceFoodImporter().importDataset(context.getConnection(), BlsCsvSource.fromClasspath());
        log.info("BLS 4.0 importiert: {} Lebensmittel", count);
    }
}
