package in.ac.iitm.guide;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The rules Spring Modulith does not cover (docs/ai/architecture-rules.md, "Enforced rules" 3 and
 * 4). {@code ModularityTest} holds the slice boundaries; the rules here hold the layering inside
 * and beneath them.
 *
 * <p>ArchUnit fails a rule whose subject set is empty, so a rule cannot pass just because the
 * package it guards has no classes.
 */
@AnalyzeClasses(packages = "in.ac.iitm.guide", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    /**
     * Dependencies point inwards: a slice may use the shared entities, the entities may not use a
     * slice. Otherwise {@code shared} could not change without touching every slice that depends on
     * it, and no slice could change without touching {@code shared}.
     *
     * <p>Phrased as "nothing outside {@code shared}" rather than as a list of the ten slices, so a
     * slice added later is covered without anyone remembering to edit this rule.
     */
    @ArchTest
    static final ArchRule shared_persistence_imports_nothing_from_a_slice = noClasses()
            .that()
            .resideInAPackage("in.ac.iitm.guide.shared.persistence..")
            .should()
            .dependOnClassesThat(
                    resideInAPackage("in.ac.iitm.guide..").and(resideOutsideOfPackage("in.ac.iitm.guide.shared..")));

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
