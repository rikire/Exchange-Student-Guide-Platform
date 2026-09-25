package in.ac.iitm.guide;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A rule Spring Modulith does not cover (docs/ai/architecture-rules.md, "Enforced rules" 4).
 * {@code ModularityTest} holds the slice boundaries; the rules here hold the layering inside and
 * beneath them.
 *
 * <p>ArchUnit fails a rule whose subject set is empty, so a rule cannot pass just because the
 * package it guards has no classes.
 */
@AnalyzeClasses(packages = "in.ac.iitm.guide", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    /**
     * {@code wikilink} is plain Java so that its tests run in milliseconds with no Spring context,
     * which is what makes writing them first cheap enough to actually do.
     */
    @ArchTest
    static final ArchRule wikilink_imports_neither_spring_nor_jpa = noClasses()
            .that()
            .resideInAPackage("in.ac.iitm.guide.wikilink..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}
