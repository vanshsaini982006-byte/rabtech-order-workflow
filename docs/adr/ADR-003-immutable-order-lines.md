# ADR-003: Immutable Order Lines and Derived Total

- Status: Accepted
- Date: 2026-10-04

## Context

An order's total is money, and money numbers that can be set from the outside are a classic source of bugs. If `total` is a stored field, it can drift from the lines it is supposed to summarize. If lines are mutable and shared, a price can change after the customer saw it and nobody knows who changed it.

## Decision

1. `OrderLine` is a record: product id, unit price and quantity, all final. The constructor rejects a quantity below 1.
2. A line is never edited in place. `withQuantity(int)` returns a new `OrderLine` with the same product and unit price, and runs the same validation.
3. `Order` exposes its lines as an unmodifiable copy (`List.copyOf`). Callers can read them but cannot add, remove or replace anything.
4. The unit price of a line is fixed when the line is added. There is no operation that changes it. To sell at a different price, the order needs a different line.
5. `Order` has no `total` field. `total()` is computed on demand as the sum of `unitPrice x quantity` over the current lines, so it can never disagree with them.
6. `Money` is a record around `BigDecimal` plus a `Currency`, kept at the currency's own scale. No `double` is used anywhere for money. Amounts with more decimals than the currency allows are rejected rather than rounded, and negative amounts and currency mixing are rejected.
7. Quantity is an `int`, so non-whole values cannot be expressed. Zero and negative values are rejected.
8. Lines can only change while the order is `DRAFT`. After confirmation, `addLine` and `changeQuantity` throw `OrderNotModifiableException`, so the total a customer confirmed is the total they pay.

## Consequences

Positive:

- The total always matches the lines. There is no setter to misuse and no cached value to invalidate.
- A quantity change cannot accidentally alter a price, because the price is carried over unchanged.
- Events such as `OrderConfirmed` can carry the total safely, because it cannot change afterwards.

Negative:

- The total is recalculated on every call. With the small number of lines in an order this is negligible; if it ever mattered, it could be cached privately without changing the public behavior.
- Changing a quantity allocates a new `OrderLine`. This is cheap and is the price of immutability.

## Interpretation notes

The assignment forbids changing a quantity "to an invalid value" but does not say whether valid quantity changes are allowed. We allow them while the order is a draft, since correcting a basket is normal, and lock the lines after confirmation. Likewise, one line per product is enforced (adding the same product twice is rejected; use `changeQuantity`), which keeps the line list unambiguous.

The transition table follows the assignment exactly. Two readings are worth stating:

- Repeating a transition (confirming a confirmed order, paying a paid one, cancelling a cancelled one) is rejected like any other invalid transition. It is not treated as a silent no-op, so a duplicate request is visible to the caller.
- `PAID -> CANCELLED` is rejected, as required. This means there is no refund path in this model; if refunds become a requirement, they should be a new state or operation rather than a loosening of this rule.
- `DRAFT` is only ever the initial state, so the public API has no operation that moves an order back to it. The rule "a paid order cannot return to draft" is still tested directly against the aggregate's transition guard.
