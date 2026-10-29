package io.github.lukmnh.toolbox.devutils;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Menegakkan dependency rule Clean Architecture di module tool-devutils. */
@AnalyzeClasses(packages = "io.github.lukmnh.toolbox.devutils", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "io.github.lukmnh.toolbox.devutils.*.domain..";
    private static final String APPLICATION = "io.github.lukmnh.toolbox.devutils.*.application..";
    private static final String INFRASTRUCTURE = "io.github.lukmnh.toolbox.devutils.*.infrastructure..";
    private static final String PRESENTATION = "io.github.lukmnh.toolbox.devutils.*.presentation..";

    /** {@code javax.crypto} (Mac, SecretKeySpec) adalah JDK standard yang dipakai aturan verifikasi JWT. */
    @ArchTest
    static final ArchRule domainUsesOnlyJdk = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "javax.crypto..", DOMAIN);

    @ArchTest
    static final ArchRule applicationUsesOnlyDomainAndTaskApi = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN, APPLICATION, "io.github.lukmnh.toolbox.common.task..");

    @ArchTest
    static final ArchRule infrastructureDoesNotKnowUseCasesOrUi = noClasses()
            .that()
            .resideInAPackage(INFRASTRUCTURE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(APPLICATION, PRESENTATION, "javafx..");

    @ArchTest
    static final ArchRule presentationDoesNotTouchLibrariesOrFileSystem = noClasses()
            .that()
            .resideInAPackage(PRESENTATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    INFRASTRUCTURE,
                    "com.fasterxml.jackson..",
                    "com.github.difflib..",
                    "com.google.re2j..",
                    "com.github.vertical_blank..",
                    "com.cronutils..")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.nio.file.Files");

    /** Folder tool (jwt, encoder, hash, checksum) tidak saling import; yang dipakai bersama ada di shared. */
    @ArchTest
    static final ArchRule toolFoldersAreIndependent = slices().matching("io.github.lukmnh.toolbox.devutils.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(
                    DescribedPredicate.<JavaClass>alwaysTrue(),
                    JavaClass.Predicates.resideInAPackage("io.github.lukmnh.toolbox.devutils.shared.."));
}
