package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class QuantityParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "200|200",
        "0.5|0.5",
        "1,5|1.5",
        "1/2|0.5",
        "½|0.5",
        "¼|0.25",
        "1½|1.5",
        "1 1/2|1.5",
        "2 1/2|2.5",
        "200-250|225",
        "8-10|9",
        "2–3|2.5",
        "2 - 3|2.5",
        "5-9|7",
        "1/2-1|0.75",
        "ca. 200|200",
        "  3  |3",
    })
    void parsesAmounts(String amount, double expected) {
        ParsedIngredient p = QuantityParser.parse(amount, "g", "Mehl");
        assertEquals(expected, p.amount(), 1e-9);
    }

    @Test
    void rangeKeepsBounds() {
        ParsedIngredient p = QuantityParser.parse("200-250", "g", "Mehl");
        assertTrue(p.isRange());
        assertEquals(200, p.amountMin());
        assertEquals(250, p.amountMax());
    }

    @Test
    void unitInNameIsExtracted() {
        ParsedIngredient p = QuantityParser.parse("1", "", "TL Fenchelsamen");
        assertEquals(1.0, p.amount());
        assertEquals("TL", p.unit());
        assertEquals("Fenchelsamen", p.name());
        assertFalse(p.unitAssumed());
    }

    @Test
    void unitInNameWithSizeWord() {
        ParsedIngredient p = QuantityParser.parse("1", "", "kleines Bund Frühlingszwiebeln");
        assertEquals("Bund", p.unit());
        assertEquals(SizeHint.SMALL, p.size());
        assertEquals("Frühlingszwiebeln", p.name());
    }

    @Test
    void sizeWordAsUnitAndUnitInName() {
        ParsedIngredient p = QuantityParser.parse("1", "kleines", "Bündel Basilikum (für das Dressing)");
        assertEquals("Bund", p.unit());
        assertEquals(SizeHint.SMALL, p.size());
        assertEquals("Basilikum (für das Dressing)", p.name());
    }

    @Test
    void pluralUnitInNameReplacesPiece() {
        ParsedIngredient p = QuantityParser.parse("2", "Stück", "Stangen Staudensellerie");
        assertEquals("Stange", p.unit());
        assertEquals("Staudensellerie", p.name());
    }

    @Test
    void cansInNameWithAlternative() {
        ParsedIngredient p = QuantityParser.parse("2", "", "Dosen weiße Bohnen (à 400 g) oder 350 g gegarte weiße Bohnen");
        assertEquals("Dose", p.unit());
        assertEquals(2.0, p.amount());
        assertTrue(p.name().startsWith("weiße Bohnen"));
    }

    @Test
    void unitInAmountField() {
        ParsedIngredient p = QuantityParser.parse("2 Stangen", "", "Porree");
        assertEquals(2.0, p.amount());
        assertEquals("Stange", p.unit());
        ParsedIngredient cup = QuantityParser.parse("1 Tasse", "", "Milch");
        assertEquals("Tasse", cup.unit());
        ParsedIngredient el = QuantityParser.parse("1 EL", null, "Mehl");
        assertEquals("EL", el.unit());
    }

    @Test
    void missingUnitFallsBackToPiece() {
        ParsedIngredient p = QuantityParser.parse("8-10", "", "Äpfel");
        assertEquals(9.0, p.amount());
        assertEquals("Stück", p.unit());
        assertTrue(p.unitAssumed());
    }

    @Test
    void trimsEverything() {
        ParsedIngredient p = QuantityParser.parse(" 1 ", "Flasche ", " Spätlese ");
        assertEquals("Flasche", p.unit());
        assertEquals("Spätlese", p.name());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Stück|Stück", "St|Stück", "St.|Stück", "Stueck|Stück", "pc|Stück", "pcs|Stück",
        "tablespoon|EL", "tablespoons|EL", "Eßl.|EL", "teaspoons|TL", "Teel.|TL",
        "P.|Päckchen", "Ml|ml", "l|l", "kg|kg", "bunches|Bund", "Zehen|Zehe", "Dosen|Dose",
        "nsp.|Msp", "Prise|Prise",
    })
    void normalizesUnitSynonyms(String raw, String expected) {
        assertEquals(expected, QuantityParser.parse("1", raw, "X").unit());
    }

    @Test
    void heapedAndSmallModifiers() {
        ParsedIngredient heaped = QuantityParser.parse("2", "gehäufte TL", "Meersalz");
        assertEquals("TL", heaped.unit());
        assertEquals(SizeHint.HEAPED, heaped.size());
        ParsedIngredient small = QuantityParser.parse("1", "small handful", "capers");
        assertEquals("Handvoll", small.unit());
        assertEquals(SizeHint.SMALL, small.size());
        ParsedIngredient thumb = QuantityParser.parse("1", "daumengroßes Stück", "Ingwer");
        assertEquals("Stück", thumb.unit());
        assertEquals(SizeHint.NONE, thumb.size());
    }

    @Test
    void sizeWordInNameIsKeptForMatchingButSetsHint() {
        ParsedIngredient p = QuantityParser.parse("1", "Stück", "große Fenchelknolle");
        assertEquals(SizeHint.LARGE, p.size());
        assertEquals("große Fenchelknolle", p.name());
    }

    @Test
    void toTasteHasNoAmount() {
        ParsedIngredient p = QuantityParser.parse(null, "nach Geschmack", "Salz");
        assertTrue(p.toTaste());
        assertNull(p.amount());
        assertNull(p.unit());
        ParsedIngredient p2 = QuantityParser.parse("", "", "Salz, Pfeffer");
        assertFalse(p2.hasAmount());
        assertNull(p2.unit());
    }

    @Test
    void unparsableAmountIsNoAmount() {
        ParsedIngredient p = QuantityParser.parse("etwas", "", "Olivenöl");
        assertNull(p.amount());
        ParsedIngredient p2 = QuantityParser.parse("n. B.", "", "Olivenöl");
        assertNull(p2.amount());
    }

    @Test
    void unknownUnitIsKeptTrimmed() {
        ParsedIngredient p = QuantityParser.parse("1", " Knäuel ", "Garn");
        assertEquals("Knäuel", p.unit());
    }

    @Test
    void divisionByZeroIsNoAmount() {
        ParsedIngredient p = QuantityParser.parse("1/0", "g", "Mehl");
        assertNull(p.amount());
    }
}
