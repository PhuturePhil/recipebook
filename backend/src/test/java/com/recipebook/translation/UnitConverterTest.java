package com.recipebook.translation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitConverterTest {

    private static String q(String amount, String unit) {
        UnitConverter.Quantity q = UnitConverter.ingredient(amount, unit);
        return (q.amount() == null ? "" : q.amount()) + " " + (q.unit() == null ? "" : q.unit());
    }

    @Test
    void cupsBecomeMillilitresRounded() {
        assertEquals("240 ml", q("1", "cup"));
        assertEquals("120 ml", q("1/2", "cups"));
        assertEquals("180 ml", q("¾", "cup"));
        assertEquals("80 ml", q("1/3", "cup"));
        assertEquals("60 ml", q("1/4", "Cup"));
        assertEquals("360 ml", q("1 1/2", "cups"));
        assertEquals("360 ml", q("1½", "cups"));
        assertEquals("1.2 l", q("5", "cups"));
    }

    @Test
    void germanNamesOfImperialUnitsCountAsImperial() {
        assertEquals("480 ml", q("2", "Tassen"));
        assertEquals("450 g", q("1", "Pfund"));
    }

    @Test
    void weightsBecomeGrams() {
        assertEquals("230 g", q("8", "oz"));
        assertEquals("400 g", q("14", "oz."));
        assertEquals("450 g", q("1", "lb"));
        assertEquals("910 g", q("2", "lbs"));
        assertEquals("1.4 kg", q("3", "pounds"));
        assertEquals("7 g", q("1/4", "ounce"));
    }

    @Test
    void fluidOuncesPintsAndInches() {
        assertEquals("240 ml", q("8", "fl oz"));
        assertEquals("470 ml", q("1", "pint"));
        assertEquals("2.5 cm", q("1", "inch"));
        assertEquals("5 cm", q("2", "inches"));
    }

    @Test
    void rangesConvertAtBothEnds() {
        assertEquals("480-720 ml", q("2-3", "cups"));
        assertEquals("480-720 ml", q("2–3", "cups"));
    }

    @Test
    void spoonsAndPiecesAreOnlyRenamed() {
        assertEquals("2 EL", q("2", "tablespoons"));
        assertEquals("1 TL", q("1", "tsp"));
        assertEquals("1/2 TL", q("1/2", "teaspoon"));
        assertEquals("3 Stück", q("3", "pcs"));
        assertEquals("1 Prise", q("1", "pinch"));
    }

    @Test
    void unreadableAmountsKeepTheirUnit() {
        assertEquals("etwas cup", q("etwas", "cup"));
        assertEquals(" cup", q(null, "cup"));
        assertEquals("400 g", q("400", "g"));
        assertEquals(" ", q(null, null));
    }

    @Test
    void fahrenheitInTextBecomesCelsius() {
        assertEquals("Den Ofen auf 180 °C vorheizen.", UnitConverter.text("Den Ofen auf 350 °F vorheizen."));
        assertEquals("Bei 200 °C backen.", UnitConverter.text("Bei 400°F backen."));
        assertEquals("Auf 220 °C erhitzen.", UnitConverter.text("Auf 425 degrees F erhitzen."));
        assertEquals("Bei 190–200 °C backen.", UnitConverter.text("Bei 375-400 °F backen."));
    }

    @Test
    void doubleTemperaturesKeepOnlyCelsius() {
        assertEquals("Bei 180 °C backen.", UnitConverter.text("Bei 180 °C (350 °F) backen."));
        assertEquals("Bei 180°C backen.", UnitConverter.text("Bei 350 °F (180°C) backen."));
    }

    @Test
    void quantitiesInTextAreConverted() {
        assertEquals("In 2,5-cm-Stücke schneiden.", UnitConverter.text("In 1-Zoll-Stücke schneiden."));
        assertEquals("In 2,5 cm große Stücke schneiden.", UnitConverter.text("In 1 inch große Stücke schneiden."));
        assertEquals("240 ml Wasser zugeben.", UnitConverter.text("1 cup Wasser zugeben."));
        assertEquals("120 ml Brühe angießen.", UnitConverter.text("1/2 Tasse Brühe angießen."));
        assertEquals("450 g Kartoffeln", UnitConverter.text("1 lb Kartoffeln"));
    }

    @Test
    void textWithoutImperialUnitsIsUnchanged() {
        String step = "1 x 400g Dose Tomaten und 150ml Olivenöl in den Topf geben, 15 Minuten köcheln. Cupcakes!";
        assertEquals(step, UnitConverter.text(step));
        assertNull(UnitConverter.text(null));
    }
}
