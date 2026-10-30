package io.github.lukmnh.toolbox.common;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.lukmnh.toolbox.common", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** common adalah fondasi: tidak boleh tahu apa pun tentang shell atau tool. */
    @ArchTest
    static final ArchRule commonDoesNotDependOnToolsOrShell = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "io.github.lukmnh.toolbox.app..",
                    "io.github.lukmnh.toolbox.pdf..",
                    "io.github.lukmnh.toolbox.json..",
                    "io.github.lukmnh.toolbox.devutils..");

    /** Utilitas non-UI harus bisa dipakai (dan dites) tanpa JavaFX. */
    @ArchTest
    static final ArchRule nonUiPackagesDoNotUseJavaFx = noClasses()
            .that()
            .resideInAnyPackage(
                    "..common.io..",
                    "..common.security..",
                    "..common.settings..",
                    "..common.i18n..",
                    "..common.report..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("javafx..");

    @ArchTest
    static final ArchRule progressAndCancellationArePureJava = noClasses()
            .that()
            .haveSimpleNameStartingWith("ProgressReporter")
            .or()
            .haveSimpleNameStartingWith("CancellationToken")
            .or()
            .haveSimpleNameStartingWith("TaskCancelledException")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("javafx..");
}
