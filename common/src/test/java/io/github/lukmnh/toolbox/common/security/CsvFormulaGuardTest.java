package io.github.lukmnh.toolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvFormulaGuardTest {

    @Test
    void formulaTriggersArePrefixed() {
        assertEquals("'=SUM(A1)", CsvFormulaGuard.neutralize("=SUM(A1)"));
        assertEquals("'+1+2", CsvFormulaGuard.neutralize("+1+2"));
        assertEquals("'-cmd", CsvFormulaGuard.neutralize("-cmd"));
        assertEquals("'@x", CsvFormulaGuard.neutralize("@x"));
        assertEquals("'\t=1", CsvFormulaGuard.neutralize("\t=1"));
        assertEquals("'\r=1", CsvFormulaGuard.neutralize("\r=1"));
    }

    @Test
    void validNumbersAreUnchanged() {
        assertEquals("-12.5", CsvFormulaGuard.neutralize("-12.5"));
        assertEquals("+62812", CsvFormulaGuard.neutralize("+62812"));
        assertEquals("-.5", CsvFormulaGuard.neutralize("-.5"));
        assertEquals("-1e5", CsvFormulaGuard.neutralize("-1e5"));
    }

    @Test
    void plainTextAndEmptyAreUnchanged() {
        assertEquals("", CsvFormulaGuard.neutralize(""));
        assertEquals("Jakarta", CsvFormulaGuard.neutralize("Jakarta"));
        assertEquals("a=b", CsvFormulaGuard.neutralize("a=b"));
        assertEquals("123", CsvFormulaGuard.neutralize("123"));
    }
}
