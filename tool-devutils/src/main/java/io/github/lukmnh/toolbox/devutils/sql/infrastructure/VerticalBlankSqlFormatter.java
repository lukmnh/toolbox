package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.FormatOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlStatementFormatter;

/** Adapter {@code com.github.vertical-blank:sql-formatter}. */
public final class VerticalBlankSqlFormatter implements SqlStatementFormatter {

    @Override
    public String format(String statement, FormatOptions options) {
        FormatConfig config = FormatConfig.builder()
                .indent(options.indent().text())
                .uppercase(options.uppercaseKeywords())
                .build();
        return SqlFormatter.of(dialect(options.dialect()))
                .format(statement, config)
                .strip();
    }

    private static Dialect dialect(SqlDialect dialect) {
        return switch (dialect) {
            case STANDARD -> Dialect.StandardSql;
            case MYSQL -> Dialect.MySql;
            case MARIADB -> Dialect.MariaDb;
            case POSTGRESQL -> Dialect.PostgreSql;
            case PLSQL -> Dialect.PlSql;
            case TSQL -> Dialect.TSql;
            case DB2 -> Dialect.Db2;
            case REDSHIFT -> Dialect.Redshift;
            case SPARK -> Dialect.SparkSql;
            case N1QL -> Dialect.N1ql;
        };
    }
}
