package io.github.lukmnh.toolbox.devutils.sql.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.GenerateOptions;
import io.github.lukmnh.toolbox.devutils.sql.domain.GeneratedSyntax;
import io.github.lukmnh.toolbox.devutils.sql.domain.SyntaxGenerator;

/** Generate INSERT/UPDATE/DELETE (PostgreSQL, Oracle) atau perintah mongosh dari data yang sudah dimuat. */
public final class GenerateSyntaxUseCase {

    /** @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan */
    public GeneratedSyntax execute(DataTable table, GenerateOptions options, CancellationToken cancellation) {
        return SyntaxGenerator.generate(table, options, cancellation::throwIfCancelled);
    }
}
