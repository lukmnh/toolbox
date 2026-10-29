package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Memecah skrip menjadi statement di {@code ;} level atas ({@code ;} di dalam string/komentar diabaikan).
 *
 * <p>Formatter library butuh waktu kuadratik terhadap panjang input (1,4 MB > 5 menit), sedangkan per statement
 * hasilnya sama karena library me-reset indentasi di setiap {@code ;}. Jadi skrip di-format per statement.
 */
public final class SqlStatementSplitter {

    private SqlStatementSplitter() {}

    /** Segmen yang hanya berisi whitespace dibuang. */
    public static List<SqlStatement> split(String sql, SqlDialect dialect) {
        List<SqlStatement> statements = new ArrayList<>();
        SqlTokenizer tokens = new SqlTokenizer(sql, dialect);
        int start = 0;
        while (tokens.hasNext()) {
            SqlToken token = tokens.next();
            if (token.type() == SqlTokenType.SEMICOLON) {
                addIfNotBlank(statements, sql.substring(start, token.end()));
                start = token.end();
            } else if (token.type() == SqlTokenType.BATCH_SEPARATOR) {
                addIfNotBlank(statements, sql.substring(start, token.start()));
                statements.add(new SqlStatement(token.text(sql), true));
                start = token.end();
            }
        }
        addIfNotBlank(statements, sql.substring(start));
        return statements;
    }

    private static void addIfNotBlank(List<SqlStatement> statements, String text) {
        if (!text.isBlank()) {
            statements.add(new SqlStatement(text, false));
        }
    }
}
