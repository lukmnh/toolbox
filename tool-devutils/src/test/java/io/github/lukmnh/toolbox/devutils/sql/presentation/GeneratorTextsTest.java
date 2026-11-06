package io.github.lukmnh.toolbox.devutils.sql.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import org.junit.jupiter.api.Test;

class GeneratorTextsTest {

    @Test
    void everyProblemHasAMessage() {
        for (Problem problem : Problem.values()) {
            assertFalse(
                    Messages.get(SqlErrorMessages.problemKey(problem)).startsWith("!"), "Key tanpa teks: " + problem);
        }
    }

    @Test
    void errorMessageIncludesFileAndLine() {
        String message =
                SqlErrorMessages.describe(new InvalidDataException("users.csv", Problem.TOO_MANY_FIELDS, 7, ""));

        assertEquals("users.csv, line 7: This row has more values than there are columns.", message);
    }

    @Test
    void defaultTableNameIsASafeIdentifier() {
        assertEquals("sales_report_2026", GeneratorViewModel.defaultTableName("Sales Report-2026.xlsx"));
        assertEquals("t_2026_data", GeneratorViewModel.defaultTableName("2026 data.csv"));
    }
}
