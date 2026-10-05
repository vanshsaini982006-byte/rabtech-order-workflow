# ADR-002: Use Ports and Adapters for External Concerns

- Status: Accepted
- Date: 2026-10-04

## Context

Handling an order touches three things outside the domain:

- storing and loading orders
- telling other parts of the system that something happened
- knowing what time it is

Each of these has many possible implementations (in-memory, SQL, message broker, system clock, test clock). The use cases should not care which one is in play, and the domain should not know any of them exist.

## Decision

The `application` module declares three small interfaces in `application.port`:

| Port | Responsibility |
|------|----------------|
| `OrderRepository` | `save(Order)` and `findById(OrderId)` |
| `NotificationPort` | `publish(DomainEvent, Instant)` |
| `ClockPort` | `now()` |

The `adapters` module implements them:

| Adapter | Implements |
|---------|------------|
| `InMemoryOrderRepository` | `OrderRepository` |
| `InMemoryNotificationAdapter` | `NotificationPort` |
| `FixedClock`, `SystemClock` | `ClockPort` |

`OrderService` receives the ports through its constructor and never refers to a concrete adapter. Dependencies point inwards: `adapters` depends on `application`, `application` depends on `domain`, and `domain` depends on nothing.

The ports live in the application module, not in the domain. They describe what a use case needs from the outside world (load this order, announce this event), which is an application concern. The domain never calls them. `Order` records its events and the service decides when to publish them.

Time is a port because "now" is an input. The domain does not read the clock at all; the service asks `ClockPort` and stamps the notification, so tests can pin time with `FixedClock` and assert exact values.

## Consequences

Positive:

- Use cases are tested with in-memory fakes, no mocking framework needed.
- A real database or message broker is added by writing a new adapter. Domain and application code do not change.
- Tests involving time are deterministic.

Negative:

- Three more interfaces to maintain, and a mapping step once a real persistence adapter exists.
- With only in-memory adapters today, the indirection looks heavier than the problem. It is kept deliberately small (one to two methods per port) so the cost stays low.

## Alternatives considered

- Calling `Instant.now()` and repositories directly from the service: simpler, but ties use cases to concrete infrastructure and makes time-based tests flaky.
- Putting the ports in the domain module: rejected because the domain has no need to call them, and it would suggest the domain owns infrastructure concerns.
