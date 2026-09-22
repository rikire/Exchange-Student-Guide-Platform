/**
 * Common ground: the JPA entities and Flyway migrations, the page layout and error handling, and the
 * security configuration. Nothing else belongs here.
 *
 * <p><strong>Open, not the Modulith default.</strong> Every slice declares its own Spring Data
 * repository over the shared entities in {@code shared.persistence} (docs/ai/architecture-rules.md,
 * "Why there are no shared repositories"), so those nested packages have to be reachable from
 * outside — and a closed Modulith module keeps its nested packages internal. Opened here, ahead of
 * the first slice repository that would otherwise fail against a boundary nobody meant to enforce
 * (DEBT-003, resolved 22 September — {@code spring-modulith-api} moved to compile scope in {@code
 * app/pom.xml} for the annotation below).
 *
 * <p>The opposite direction — "shared -&gt; slice forbidden", rule 3 in architecture-rules.md — is
 * checked by ArchUnit rather than by Modulith, and is unaffected.
 *
 * <p><strong>Growing this package is a stop-and-ask trigger.</strong> {@code shared} is the one
 * place where two people collide, so it grows by decision and never by convenience: if exactly one
 * slice uses something, it belongs to that slice.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package in.ac.iitm.guide.shared;
