# Order Workflow

A small Java 17 / Maven multi-module project that models an order approval and payment workflow. The point of the exercise is the shape of the code: all business rules live in a framework-free domain model, and everything else (persistence, notifications, time) sits behind ports.

There is no Spring, no database and no web layer. It builds and tests with plain Maven.

## Business rules

1. An order must contain at least one line before it can be confirmed.
2. A line's quantity must be a positive whole number.
3. A cancelled order cannot be paid.
4. A paid order cannot return to draft.
5. The order total is always derived from the lines' immutable unit prices and quantities.
6. Invalid state transitions are rejected explicitly with a domain exception.
7. All of this is enforced inside the `Order` aggregate, not in services or adapters.

## State transitions

```
            confirm()                 recordPayment()
  DRAFT ----------------> CONFIRMED ------------------> PAID
    |                         |
    | cancel()                | cancel()
    v                         v
  CANCELLED <-----------------+
```

| From \ To   | DRAFT | CONFIRMED | PAID | CANCELLED |
|-------------|:-----:|:---------:|:----:|:---------:|
| DRAFT       |   no  |    yes    |  no  |    yes    |
| CONFIRMED   |   no  |     no    |  yes |    yes    |
| PAID        |   no  |     no    |  no  |     no    |
| CANCELLED   |   no  |     no    |  no  |     no    |

`PAID` and `CANCELLED` are terminal. A successful transition records one domain event:

| Call               | Event            |
|--------------------|------------------|
| `confirm()`        | `OrderConfirmed` |
| `recordPayment()`  | `PaymentRecorded`|
| `cancel()`         | `OrderCancelled` |

A rejected call changes nothing and records no event.

## Architecture

```
  +--------------------------------------------------------+
  |  adapters                                              |
  |  InMemoryOrderRepository  InMemoryNotificationAdapter  |
  |  FixedClock  SystemClock                               |
  +---------------------------+----------------------------+
                              | implements ports
                              v
  +--------------------------------------------------------+
  |  application                                           |
  |  OrderService     port: OrderRepository                |
  |                   port: NotificationPort               |
  |                   port: ClockPort                      |
  +---------------------------+----------------------------+
                              | uses
                              v
  +--------------------------------------------------------+
  |  domain  (plain Java 17, no dependencies)              |
  |  Order  OrderLine  Money  OrderStatus  domain events   |
  +--------------------------------------------------------+

  Dependencies point inwards only: adapters -> application -> domain
```

The application layer coordinates (load, call the aggregate, save, publish). The domain decides.

## Modules

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `domain` | `order-domain` | The `Order` aggregate, value objects, domain events and domain exceptions. No dependencies besides JUnit (test scope). |
| `application` | `order-application` | `OrderService` use cases and the three ports. Depends on `domain`. |
| `adapters` | `order-adapters` | In-memory implementations of the ports, plus a wiring test that exercises the whole flow. Depends on `application` and `domain`. |

## Package structure

```
com.rabtech.orderworkflow
├── domain
│   ├── DomainException
│   ├── money
│   │   ├── Money
│   │   └── InvalidMoneyException
│   └── order
│       ├── Order                          aggregate root
│       ├── OrderLine                      immutable line (record)
│       ├── OrderId, ProductId             identifiers
│       ├── OrderStatus                    states and legal moves
│       ├── DomainEvent                    sealed interface
│       ├── OrderConfirmed, PaymentRecorded, OrderCancelled
│       └── InvalidOrderTransitionException, EmptyOrderException,
│           InvalidOrderLineException, OrderNotModifiableException
├── application
│   ├── OrderService
│   ├── OrderNotFoundException
│   └── port
│       ├── OrderRepository
│       ├── NotificationPort
│       └── ClockPort
└── adapters
    ├── persistence   InMemoryOrderRepository
    ├── notification  InMemoryNotificationAdapter
    └── time          FixedClock, SystemClock
```

## Build and test

Requirements: JDK 17 or newer and Maven 3.8+.

```
mvn clean verify     # compile and run every test in all modules
mvn test             # tests only
mvn -pl domain test  # just the domain module
```

## How the domain stays framework independent

- `domain/pom.xml` declares one dependency: JUnit Jupiter, test scope. Nothing else.
- Modules depend inwards only; `domain` has no dependency on `application` or `adapters`.
- `DomainPurityTest` runs with the domain tests and fails the build if:
  - a main source file imports anything outside `java.math`, `java.util` or the domain's own packages,
  - the domain pom declares any dependency other than test-scoped JUnit,
  - well-known Spring, JPA, Hibernate or JDBC driver classes show up on the domain classpath,
  - a domain source file refers to the `application` or `adapters` packages.

The reasoning is recorded in [ADR-001](docs/adr/ADR-001-domain-purity.md).

## Example usage

```java
OrderService service = new OrderService(
        new InMemoryOrderRepository(),
        new InMemoryNotificationAdapter(),
        new SystemClock());

OrderId id = service.createDraft(Currency.getInstance("USD"));
service.addLine(id, new ProductId("KEYBOARD"), Money.of("49.99", "USD"), 1);
service.addLine(id, new ProductId("MOUSE"), Money.of("19.50", "USD"), 2);

service.confirm(id);        // publishes OrderConfirmed
service.recordPayment(id);  // publishes PaymentRecorded

service.getOrder(id).total();   // USD 88.99
service.cancel(id);             // throws InvalidOrderTransitionException (PAID -> CANCELLED)
```

Using the aggregate directly, with no application layer at all:

```java
Order order = Order.draft(OrderId.generate(), Currency.getInstance("USD"));
order.addLine(new ProductId("KEYBOARD"), Money.of("49.99", "USD"), 2);
order.confirm();
order.pullDomainEvents();   // [OrderConfirmed[...]]
```

## Test coverage summary

112 tests, all JUnit 5.

| Module | Tests | What they cover |
|--------|------:|-----------------|
| `domain` | 89 | `OrderTest` (49): initial state, lines, quantity rules, confirmation, payment, cancellation, every invalid transition, total, immutability, events and no events on failure. `OrderStatusTest` (16): all 16 from/to pairs against the transition table. `OrderLineTest` (10): quantity validation, line total, `withQuantity`. `MoneyTest` (10): scale, exact addition, rejection of negatives, over-precise amounts and mixed currencies. `DomainPurityTest` (4): dependency rules above. |
| `application` | 11 | `OrderService` with fakes: saving, publishing the right event with the clock's time, nothing published on a rejected transition, unknown order. |
| `adapters` | 12 | In-memory repository save and retrieve, notification adapter, clocks, and an end-to-end flow wired with the real adapters. |

The 20 behaviours listed in the assignment are all covered in `OrderTest`, `OrderLineTest` and `InMemoryOrderRepositoryTest`.

## Architecture decision records

- [ADR-001: Keep Domain Model Framework Independent](docs/adr/ADR-001-domain-purity.md)
- [ADR-002: Use Ports and Adapters for External Concerns](docs/adr/ADR-002-ports-and-adapters.md)
- [ADR-003: Immutable Order Lines and Derived Total](docs/adr/ADR-003-immutable-order-lines.md)

## Design decisions

- **Money is `BigDecimal` plus `Currency`**, never `double`. Values are held at the currency's scale, so `10` and `10.00` are equal. Amounts with too many decimals are rejected, not rounded, so a pricing mistake surfaces instead of silently losing cents.
- **Behavior methods, no setters.** `confirm()`, `recordPayment()` and `cancel()` are the only ways to change status. The guard that checks a move lives in `OrderStatus.canMoveTo` and is applied by the aggregate.
- **Events are collected by the aggregate and pulled by the caller.** `Order` records an event on each successful transition; `OrderService` pulls them after saving and hands them to the `NotificationPort`. No event framework is involved, and the domain does not publish anything itself.
- **Time is passed in, not read.** The domain never calls `Instant.now()`. The service takes the time from `ClockPort`.
- **Lines are editable only in `DRAFT`.** Once confirmed, the total is fixed. One line per product; use `changeQuantity` to adjust.
- **`DomainEvent` is a sealed interface**, so the set of events is closed and a `switch` over them can be checked by the compiler.
- **Repeated transitions are rejected**, not ignored. See ADR-003 for this and the missing refund path.
