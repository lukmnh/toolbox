package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlMinifier;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlOutput;

/** Minify SQL dengan tokenizer sendiri; isi string literal dan quoted identifier tidak berubah. */
public final class MinifySqlUseCase {

    /** @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan */
    public SqlOutput execute(String sql, SqlDialect dialect, boolean removeComments, CancellationToken cancellation) {
        return new SqlOutput(SqlMinifier.minify(sql, dialect, removeComments, cancellation::throwIfCancelled), 0);
    }
}
