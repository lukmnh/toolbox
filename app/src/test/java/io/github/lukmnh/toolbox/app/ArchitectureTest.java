package io.github.lukmnh.toolbox.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.lukmnh.toolbox.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Shell hanya mengenal kontrak Tool; tool ditemukan lewat ServiceLoader, bukan di-import. */
    @ArchTest
    static final ArchRule shellDoesNotImportToolModules = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "io.github.lukmnh.toolbox.pdf..",
                    "io.github.lukmnh.toolbox.json..",
                    "io.github.lukmnh.toolbox.devutils..",
                    "io.github.lukmnh.toolbox.productivity..");
}
