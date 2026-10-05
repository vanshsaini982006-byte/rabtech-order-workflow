# ADR-001: Keep Domain Model Framework Independent

- Status: Accepted
- Date: 2026-10-04

## Context

The order workflow has real business rules: an order needs a line before it can be confirmed, a cancelled order can never be paid, a paid order never goes back. These rules are the part of the system most likely to outlive any technology choice, and the part we most need to test quickly and with confidence.

Frameworks pull in the opposite direction. JPA wants a no-arg constructor, mutable fields and setters. Spring wants annotations and a container. HTTP and JSON libraries want public getters and default constructors. If the `Order` class has to satisfy those demands it stops being able to protect its own invariants, and tests need a container or a database to run.

## Decision

The `domain` module is plain Java 17. Its only dependency is JUnit 5 in test scope.

It must not depend on, import or reference:

- Spring or Spring Boot
- JPA, Hibernate or any ORM
- JDBC, MySQL or PostgreSQL drivers
- HTTP or REST frameworks, JSON or serialization libraries
- the `application` or `adapters` modules

The rule is enforced in two places:

1. Maven: `domain/pom.xml` declares no compile-scope dependencies, and the module graph only points inwards (`adapters` -> `application` -> `domain`).
2. `DomainPurityTest`: fails the build if a domain source file imports anything outside `java.math`, `java.util` and the domain's own packages, if the pom declares anything besides test-scoped JUnit, or if the domain mentions the outer packages.

## Consequences

Positive:

- Every business rule is tested with `new` and plain assertions, in milliseconds.
- `Order` can enforce its invariants. There are no setters, no framework-required constructors and no proxies.
- Swapping persistence, messaging or the web layer never touches the domain.

Negative:

- A persistence adapter has to map between the `Order` aggregate and whatever storage model it uses. That mapping code is real work that an annotated entity would hide.
- Features that frameworks give for free (auditing, bean validation, change tracking) are not available inside the domain and have to be written by hand or provided by adapters.

We accept both costs. The mapping lives at the edge, where it belongs.

## Why Spring and JPA are excluded specifically

- JPA entities need to be mutable and proxyable. That conflicts directly with an aggregate that guards its own state transitions and with immutable order lines (see ADR-003).
- Annotations such as `@Entity` or `@Component` tie a business class to one vendor's lifecycle. A domain class should compile and run without those jars on the classpath.
- Spring and JPA are infrastructure. They are good at wiring and storing objects, but they have no opinion on whether an order may be paid, so they have no business inside the class that decides it.
