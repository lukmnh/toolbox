package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.viewer.domain.ByteSearch;
import io.github.lukmnh.toolbox.json.viewer.domain.Query;
import io.github.lukmnh.toolbox.json.viewer.domain.QueryParser;
import io.github.lukmnh.toolbox.json.viewer.domain.ViewFilter;
import java.util.Arrays;

/**
 * Menyaring semua record dengan quick search (byte mentah, tanpa parse) dan filter query. Record dibaca satu
 * per satu dari file sehingga memori tetap kecil.
 */
public final class FilterRecordsUseCase {

    private static final int CHECK_INTERVAL = 1024;

    /**
     * @return nomor record yang cocok, urut sesuai file
     * @throws io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException bila query salah, sebelum
     *     record dipindai
     */
    public int[] execute(
            ViewerSession session, ViewFilter filter, ProgressReporter progress, CancellationToken cancellation) {
        if (filter.isEmpty()) {
            return session.allRows();
        }
        Query query = filter.hasQuery() ? QueryParser.parse(filter.query()) : null;
        byte[] needle = filter.hasQuickSearch() ? ByteSearch.needle(filter.quickSearch()) : null;
        int count = session.recordCount();
        int[] matches = new int[count];
        int found = 0;
        for (int record = 0; record < count; record++) {
            if (record % CHECK_INTERVAL == 0) {
                cancellation.throwIfCancelled();
                progress.report(record, count);
            }
            if (matches(session, record, needle, query)) {
                matches[found++] = record;
            }
        }
        progress.report(count, count);
        return Arrays.copyOf(matches, found);
    }

    private static boolean matches(ViewerSession session, int record, byte[] needle, Query query) {
        byte[] raw = session.raw(record);
        if (needle != null && !ByteSearch.contains(raw, needle)) {
            return false;
        }
        if (query == null) {
            return true;
        }
        try {
            return query.matches(session.parse(raw));
        } catch (MalformedJsonException e) {
            // Record rusak (misalnya satu baris JSONL) tidak cocok dengan query apa pun
            return false;
        }
    }
}
