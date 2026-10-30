package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;

/** Baris mock yang deterministik per seed; lazy (Iterator) supaya 100k baris tidak ditahan di memori. */
public final class MockGenerator {

    private MockGenerator() {}

    /**
     * @return nilai per baris, urut sesuai kolom, sudah berbentuk teks siap format
     * @throws InvalidMockSchemaException bila schema tidak valid
     */
    public static Iterator<List<String>> rows(MockSchema schema) {
        MockSchemaValidator.validate(schema);
        SplittableRandom random = new SplittableRandom(schema.seed());
        List<ColumnSource> sources = schema.columns().stream()
                .map(column -> ColumnSource.of(column, schema.locale()))
                .toList();
        return new Iterator<>() {
            private int row;

            @Override
            public boolean hasNext() {
                return row < schema.rows();
            }

            @Override
            public List<String> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                row++;
                int current = row;
                return sources.stream()
                        .map(source -> source.next(random, current))
                        .toList();
            }
        };
    }
}
