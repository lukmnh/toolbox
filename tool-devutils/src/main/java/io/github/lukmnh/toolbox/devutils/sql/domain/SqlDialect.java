package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.EnumSet;
import java.util.Set;

/** Dialect yang didukung formatter. Urutan enum = urutan tampil di pilihan dialect. */
public enum SqlDialect {
    STANDARD("Standard SQL", EnumSet.noneOf(SqlSyntax.class)),
    MYSQL(
            "MySQL",
            EnumSet.of(SqlSyntax.BACKSLASH_ESCAPES, SqlSyntax.HASH_COMMENTS, SqlSyntax.DASH_COMMENT_NEEDS_SPACE)),
    MARIADB(
            "MariaDB",
            EnumSet.of(SqlSyntax.BACKSLASH_ESCAPES, SqlSyntax.HASH_COMMENTS, SqlSyntax.DASH_COMMENT_NEEDS_SPACE)),
    POSTGRESQL(
            "PostgreSQL",
            EnumSet.of(SqlSyntax.DOLLAR_QUOTES, SqlSyntax.ESCAPE_STRING_PREFIX, SqlSyntax.NESTED_COMMENTS)),
    PLSQL("Oracle PL/SQL", EnumSet.of(SqlSyntax.ALTERNATIVE_QUOTING)),
    TSQL(
            "SQL Server T-SQL",
            EnumSet.of(SqlSyntax.BRACKET_IDENTIFIERS, SqlSyntax.NESTED_COMMENTS, SqlSyntax.GO_BATCH_SEPARATOR)),
    DB2("IBM Db2", EnumSet.noneOf(SqlSyntax.class)),
    REDSHIFT("Amazon Redshift", EnumSet.of(SqlSyntax.DOLLAR_QUOTES)),
    SPARK("Spark SQL", EnumSet.of(SqlSyntax.BACKSLASH_ESCAPES)),
    N1QL("Couchbase N1QL", EnumSet.of(SqlSyntax.BACKSLASH_ESCAPES));

    private final String displayName;
    private final Set<SqlSyntax> syntax;

    SqlDialect(String displayName, Set<SqlSyntax> syntax) {
        this.displayName = displayName;
        this.syntax = syntax;
    }

    /** Nama produk, tidak diterjemahkan. */
    public String displayName() {
        return displayName;
    }

    public boolean supports(SqlSyntax feature) {
        return syntax.contains(feature);
    }
}
