package com.recipebook.nutrition;

import java.util.List;

/**
 * Austauschbare Quelle für Referenz-Nährwerte (pro 100 g). Heute: BLS 4.0 als CSV im Classpath.
 */
public interface ReferenceFoodSource {

    DatasetInfo dataset();

    List<ReferenceFoodRow> rows();
}
