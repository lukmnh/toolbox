package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.util.List;

/**
 * @param seed seed yang sama menghasilkan data yang sama
 * @param table nama tabel untuk format SQL
 */
public record MockSchema(List<MockColumn> columns, MockLocale locale, long seed, int rows, String table) {

    public MockSchema {
        columns = List.copyOf(columns);
    }
}
