package io.github.lukmnh.toolbox.pdf;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Menegakkan dependency rule Clean Architecture di module tool-pdf. */
@AnalyzeClasses(packages = "io.github.lukmnh.toolbox.pdf", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "io.github.lukmnh.toolbox.pdf.*.domain..";
    private static final String APPLICATION = "io.github.lukmnh.toolbox.pdf.*.application..";
    private static final String INFRASTRUCTURE = "io.github.lukmnh.toolbox.pdf.*.infrastructure..";
    private static final String PRESENTATION = "io.github.lukmnh.toolbox.pdf.*.presentation..";

    @ArchTest
    static final ArchRule domainUsesOnlyJdk = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN);

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
    static final ArchRule presentationDoesNotTouchPdfBoxOrFileSystem = noClasses()
            .that()
            .resideInAPackage(PRESENTATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(INFRASTRUCTURE, "org.apache.pdfbox..")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.nio.file.Files");

    /** Folder tool (merge, split, compress, sign) tidak saling import; yang dipakai bersama ada di shared. */
    @ArchTest
    static final ArchRule toolFoldersAreIndependent = slices().matching("io.github.lukmnh.toolbox.pdf.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(
                    DescribedPredicate.<JavaClass>alwaysTrue(),
                    JavaClass.Predicates.resideInAPackage("io.github.lukmnh.toolbox.pdf.shared.."));
}
