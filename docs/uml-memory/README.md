# UML Memory

## Objective

This folder contains a reverse-engineered UML documentation set rebuilt from the real codebase.
It is intended to preserve a stable architectural memory of the project and to make future refactors easier to reason about.

## UML Standard

All diagrams are modeled with the intent to stay compatible with UML 2.5.1 concepts and notation.
The PlantUML sources use standard UML diagram families only:

- Use Case Diagram
- Class Diagram
- Sequence Diagram
- Component Diagram

## Generated Diagrams

- `use-case-diagram.puml`
- `class-diagram.puml`
- `sequence-01-main-flow.puml`
- `sequence-02-monitoring-flow.puml`
- `sequence-03-zkbio-flow.puml`
- `architecture-overview.puml`

## Reverse-Engineering Method

The documentation was reconstructed from:

- backend packages under `src/main/java/tn/iteam`
- frontend monitoring code under `frontend/src/app`
- Spring controllers, services, repositories, schedulers, configuration, and websocket publishers
- adapters and external clients
- JPA entities and repositories
- test classes under `src/test/java/tn/iteam`
- application properties and runtime configuration

The reconstruction favored code that is actively wired and reachable over legacy comments or obsolete assumptions.

## Scope

The resulting UML focuses on the parts that structure the system:

- unified monitoring flow
- Zabbix, Observium, ZKBio, and Camera integrations
- snapshot-based aggregation
- websocket publication
- dashboard analytics and ML prediction
- ticket workflow
- frontend monitoring consumers
- optional Redis configuration

## Known Limits

- Some compatibility controllers still exist and are documented as compatibility, not as target architecture.
- The repository contains more DTOs, mappers, and utility classes than a readable UML should expose; diagrams intentionally keep only the structurally significant elements.
- Runtime infrastructure outside the repository, such as real deployment topology, authentication integration, or external network topology, cannot be fully derived from code alone.
- Some tests still contain historical freshness labels that no longer match the current runtime semantics; diagrams follow the production code path first.
