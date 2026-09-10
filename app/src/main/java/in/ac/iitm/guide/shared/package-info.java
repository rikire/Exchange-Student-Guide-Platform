/**
 * Common ground: the JPA entities and Flyway migrations, the page layout and error handling, and the
 * security configuration. Nothing else belongs here.
 *
 * <p><strong>This module is currently closed, and it will have to be open.</strong> Every slice
 * declares its own Spring Data repository over the shared entities in {@code shared.persistence}
 * (docs/ai/architecture-rules.md, "Why there are no shared repositories"), so those nested packages
 * have to be reachable from outside — and a closed Modulith module keeps its nested packages
 * internal. Nothing enforces the difference yet, because this package is empty: the conflict appears
 * the moment the first entity and the first slice repository exist, in phase 2. Recorded as
 * DEBT-003 rather than pre-empted here, because fixing it needs {@code spring-modulith-api} at
 * compile scope, and adding a dependency is a decision to take when it is actually needed.
 *
 * <p>The opposite direction — "shared -&gt; slice forbidden", rule 3 in architecture-rules.md — is
 * checked by ArchUnit rather than by Modulith, and is unaffected.
 *
 * <p><strong>Growing this package is a stop-and-ask trigger.</strong> {@code shared} is the one
 * place where two people collide, so it grows by decision and never by convenience: if exactly one
 * slice uses something, it belongs to that slice.
 *
 * <p><strong>Declared, not yet built</strong> — as with the ten slices.
 */
package in.ac.iitm.guide.shared;
