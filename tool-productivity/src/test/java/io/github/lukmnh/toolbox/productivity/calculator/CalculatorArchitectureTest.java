package io.github.lukmnh.toolbox.productivity.calculator;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Menegakkan dependency rule Clean Architecture di package calculator (Installment Simulator). */
@AnalyzeClasses(
        packages = "io.github.lukmnh.toolbox.productivity.calculator",
        importOptions = ImportOption.DoNotIncludeTests.class)
class CalculatorArchitectureTest {

    private static final String ROOT = "io.github.lukmnh.toolbox.productivity.calculator";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";
    private static final String PRESENTATION = ROOT + ".presentation..";
    private static final String SHARED_DOMAIN = "io.github.lukmnh.toolbox.productivity.shared.domain..";

    @ArchTest
    static final ArchRule domainUsesOnlyJdk = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN);

    @ArchTest
    static final ArchRule applicationUsesOnlyDomain = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN, APPLICATION, SHARED_DOMAIN);

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
            .resideInAnyPackage(INFRASTRUCTURE, "org.apache.pdfbox..", "de.siegmar.fastcsv..")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("io.github.lukmnh.toolbox.common.report.PdfReport")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.nio.file.Files");
}
