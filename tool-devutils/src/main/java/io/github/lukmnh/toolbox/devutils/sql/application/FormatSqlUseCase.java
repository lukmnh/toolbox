package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFormatFailedException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlOutput;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlStatement;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlStatementFormatter;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlStatementSplitter;
import java.util.List;

/** Format skrip SQL per statement supaya waktu proses linear dan bisa dibatalkan di antara statement. */
public final class FormatSqlUseCase {

    /**
     * Statement yang lebih panjang ditulis apa adanya: waktu format library kuadratik (64 ribu karakter ≈ 1,3 detik,
     * 120 ribu ≈ 3,5 detik) dan library tidak bisa diinterupsi.
     */
    public static final int MAX_STATEMENT_CHARS = 64_000;

    private final SqlStatementFormatter formatter;

    public FormatSqlUseCase(SqlStatementFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @throws SqlFormatFailedException bila library gagal pada salah satu statement
     * @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan
     */
    public SqlOutput execute(String sql, FormatOptions options, CancellationToken cancellation) {
        List<SqlStatement> statements = SqlStatementSplitter.split(sql, options.dialect());
        String separator = "\n".repeat(options.blankLinesBetweenQueries() + 1);
        StringBuilder out = new StringBuilder(sql.length() + sql.length() / 2);
        int unformatted = 0;
        for (int index = 0; index < statements.size(); index++) {
            cancellation.throwIfCancelled();
            if (!out.isEmpty()) {
                out.append(separator);
            }
            SqlStatement statement = statements.get(index);
            boolean tooLong = statement.text().length() > MAX_STATEMENT_CHARS;
            if (statement.batchSeparator() || tooLong) {
                out.append(statement.text().strip());
                unformatted += tooLong ? 1 : 0;
            } else {
                out.append(formatStatement(statement.text(), options, index + 1));
            }
        }
        if (!out.isEmpty()) {
            out.append('\n');
        }
        return new SqlOutput(out.toString(), unformatted);
    }

    private String formatStatement(String statement, FormatOptions options, int number) {
        try {
            return formatter.format(statement, options);
        } catch (RuntimeException e) {
            throw new SqlFormatFailedException(number, e);
        }
    }
}
