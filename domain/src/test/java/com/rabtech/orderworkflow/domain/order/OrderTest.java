package com.rabtech.orderworkflow.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabtech.orderworkflow.domain.money.Money;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final ProductId KEYBOARD = new ProductId("KEYBOARD");
    private static final ProductId MOUSE = new ProductId("MOUSE");

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }

    private static Order newOrder() {
        return Order.draft(OrderId.generate(), USD);
    }

    private static Order draftWithLine() {
        Order order = newOrder();
        order.addLine(KEYBOARD, usd("49.99"), 1);
        return order;
    }

    private static Order confirmedOrder() {
        Order order = draftWithLine();
        order.confirm();
        order.pullDomainEvents();
        return order;
    }

    private static Order paidOrder() {
        Order order = confirmedOrder();
        order.recordPayment();
        order.pullDomainEvents();
        return order;
    }

    private static Order cancelledOrder() {
        Order order = draftWithLine();
        order.cancel();
        order.pullDomainEvents();
        return order;
    }

    private static Order orderIn(OrderStatus status) {
        return switch (status) {
            case DRAFT -> draftWithLine();
            case CONFIRMED -> confirmedOrder();
            case PAID -> paidOrder();
            case CANCELLED -> cancelledOrder();
        };
    }

    @Test
    void newOrderStartsAsDraft() {
        Order order = newOrder();

        assertEquals(OrderStatus.DRAFT, order.status());
        assertTrue(order.lines().isEmpty());
    }

    @Test
    void orderCanContainLines() {
        Order order = newOrder();

        order.addLine(KEYBOARD, usd("49.99"), 1);
        order.addLine(MOUSE, usd("19.50"), 2);

        assertEquals(List.of(
                new OrderLine(KEYBOARD, usd("49.99"), 1),
                new OrderLine(MOUSE, usd("19.50"), 2)), order.lines());
    }

    @Test
    void quantityOfOneIsAccepted() {
        Order order = newOrder();

        order.addLine(KEYBOARD, usd("49.99"), 1);

        assertEquals(1, order.lines().get(0).quantity());
    }

    @Test
    void quantityGreaterThanOneIsAccepted() {
        Order order = newOrder();

        order.addLine(KEYBOARD, usd("49.99"), 12);

        assertEquals(12, order.lines().get(0).quantity());
    }

    @Test
    void quantityOfZeroIsRejected() {
        Order order = newOrder();

        assertThrows(InvalidOrderLineException.class, () -> order.addLine(KEYBOARD, usd("49.99"), 0));
        assertTrue(order.lines().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, Integer.MIN_VALUE})
    void negativeQuantityIsRejected(int quantity) {
        Order order = newOrder();

        assertThrows(InvalidOrderLineException.class, () -> order.addLine(KEYBOARD, usd("49.99"), quantity));
        assertTrue(order.lines().isEmpty());
    }

    @Test
    void sameProductCannotBeAddedTwice() {
        Order order = draftWithLine();

        assertThrows(InvalidOrderLineException.class, () -> order.addLine(KEYBOARD, usd("49.99"), 1));
        assertEquals(1, order.lines().size());
    }

    @Test
    void lineInAnotherCurrencyIsRejected() {
        Order order = newOrder();

        assertThrows(InvalidOrderLineException.class, () -> order.addLine(KEYBOARD, Money.of("40.00", "EUR"), 1));
    }

    @Test
    void emptyOrderCannotBeConfirmed() {
        Order order = newOrder();

        assertThrows(EmptyOrderException.class, order::confirm);
        assertEquals(OrderStatus.DRAFT, order.status());
    }

    @Test
    void orderWithALineCanBeConfirmed() {
        Order order = draftWithLine();

        order.confirm();

        assertEquals(OrderStatus.CONFIRMED, order.status());
    }

    @Test
    void confirmedOrderCanBePaid() {
        Order order = confirmedOrder();

        order.recordPayment();

        assertEquals(OrderStatus.PAID, order.status());
    }

    @Test
    void draftOrderCanBeCancelled() {
        Order order = draftWithLine();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void confirmedOrderCanBeCancelled() {
        Order order = confirmedOrder();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void draftOrderCannotBePaid() {
        Order order = draftWithLine();

        InvalidOrderTransitionException error = assertThrows(InvalidOrderTransitionException.class, order::recordPayment);

        assertEquals(OrderStatus.DRAFT, error.from());
        assertEquals(OrderStatus.PAID, error.to());
        assertEquals(OrderStatus.DRAFT, order.status());
    }

    @Test
    void cancelledOrderCannotBePaid() {
        Order order = cancelledOrder();

        InvalidOrderTransitionException error = assertThrows(InvalidOrderTransitionException.class, order::recordPayment);

        assertEquals(OrderStatus.CANCELLED, error.from());
        assertEquals(OrderStatus.PAID, error.to());
        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void cancelledOrderCannotBeConfirmed() {
        Order order = cancelledOrder();

        assertThrows(InvalidOrderTransitionException.class, order::confirm);
        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void paidOrderCannotBeConfirmedAgain() {
        Order order = paidOrder();

        assertThrows(InvalidOrderTransitionException.class, order::confirm);
        assertEquals(OrderStatus.PAID, order.status());
    }

    @Test
    void paidOrderCannotBeCancelled() {
        Order order = paidOrder();

        assertThrows(InvalidOrderTransitionException.class, order::cancel);
        assertEquals(OrderStatus.PAID, order.status());
    }

    @Test
    void confirmedOrderCannotBeConfirmedTwice() {
        Order order = confirmedOrder();

        assertThrows(InvalidOrderTransitionException.class, order::confirm);
    }

    @Test
    void paidOrderCannotBePaidTwice() {
        Order order = paidOrder();

        assertThrows(InvalidOrderTransitionException.class, order::recordPayment);
    }

    @Test
    void cancelledOrderCannotBeCancelledTwice() {
        Order order = cancelledOrder();

        assertThrows(InvalidOrderTransitionException.class, order::cancel);
    }

    @Test
    void paidOrderCannotReturnToDraft() {
        Order order = paidOrder();

        InvalidOrderTransitionException error = assertThrows(
                InvalidOrderTransitionException.class, () -> order.transitionTo(OrderStatus.DRAFT));

        assertEquals(OrderStatus.PAID, error.from());
        assertEquals(OrderStatus.DRAFT, error.to());
        assertEquals(OrderStatus.PAID, order.status());
    }

    @ParameterizedTest
    @MethodSource("everyStatus")
    void noOrderCanReturnToDraft(OrderStatus status) {
        Order order = orderIn(status);

        assertThrows(InvalidOrderTransitionException.class, () -> order.transitionTo(OrderStatus.DRAFT));
        assertEquals(status, order.status());
    }

    private static Stream<OrderStatus> everyStatus() {
        return Stream.of(OrderStatus.values());
    }

    @Test
    void totalIsSumOfUnitPriceTimesQuantity() {
        Order order = newOrder();
        order.addLine(KEYBOARD, usd("49.99"), 2);
        order.addLine(MOUSE, usd("19.50"), 3);

        assertEquals(usd("158.48"), order.total());
    }

    @Test
    void totalOfEmptyOrderIsZero() {
        assertEquals(Money.zero(USD), newOrder().total());
    }

    @Test
    void totalFollowsQuantityChange() {
        Order order = newOrder();
        order.addLine(KEYBOARD, usd("10.00"), 1);
        order.addLine(MOUSE, usd("0.10"), 3);

        order.changeQuantity(MOUSE, 10);

        assertEquals(usd("11.00"), order.total());
    }

    @Test
    void unitPriceIsUnchangedWhenQuantityChanges() {
        Order order = draftWithLine();
        Money priceBefore = order.lines().get(0).unitPrice();

        order.changeQuantity(KEYBOARD, 5);

        assertEquals(priceBefore, order.lines().get(0).unitPrice());
        assertEquals(usd("249.95"), order.total());
    }

    @Test
    void linesViewCannotBeUsedToChangeTheOrder() {
        Order order = draftWithLine();
        List<OrderLine> view = order.lines();

        assertThrows(UnsupportedOperationException.class, () -> view.add(new OrderLine(MOUSE, usd("1.00"), 1)));
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertEquals(1, order.lines().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void quantityCannotBeChangedToAnInvalidValue(int invalid) {
        Order order = draftWithLine();

        assertThrows(InvalidOrderLineException.class, () -> order.changeQuantity(KEYBOARD, invalid));
        assertEquals(1, order.lines().get(0).quantity());
    }

    @Test
    void quantityCannotBeChangedForUnknownProduct() {
        Order order = draftWithLine();

        assertThrows(InvalidOrderLineException.class, () -> order.changeQuantity(MOUSE, 2));
    }

    @Test
    void linesCannotBeChangedOnceConfirmed() {
        Order order = confirmedOrder();

        assertThrows(OrderNotModifiableException.class, () -> order.addLine(MOUSE, usd("19.50"), 1));
        assertThrows(OrderNotModifiableException.class, () -> order.changeQuantity(KEYBOARD, 3));
        assertEquals(usd("49.99"), order.total());
    }

    @Test
    void linesCannotBeChangedOnceCancelled() {
        Order order = cancelledOrder();

        assertThrows(OrderNotModifiableException.class, () -> order.addLine(MOUSE, usd("19.50"), 1));
    }

    @Test
    void confirmingRaisesOrderConfirmed() {
        Order order = draftWithLine();

        order.confirm();

        assertEquals(List.of(new OrderConfirmed(order.id(), usd("49.99"))), order.pullDomainEvents());
    }

    @Test
    void recordingPaymentRaisesPaymentRecorded() {
        Order order = confirmedOrder();

        order.recordPayment();

        assertEquals(List.of(new PaymentRecorded(order.id(), usd("49.99"))), order.pullDomainEvents());
    }

    @Test
    void cancellingRaisesOrderCancelled() {
        Order order = confirmedOrder();

        order.cancel();

        assertEquals(List.of(new OrderCancelled(order.id(), OrderStatus.CONFIRMED)), order.pullDomainEvents());
    }

    @Test
    void cancellingADraftRecordsThatItWasADraft() {
        Order order = draftWithLine();

        order.cancel();

        assertEquals(List.of(new OrderCancelled(order.id(), OrderStatus.DRAFT)), order.pullDomainEvents());
    }

    @Test
    void failedConfirmationRaisesNoEvent() {
        Order order = newOrder();

        assertThrows(EmptyOrderException.class, order::confirm);

        assertTrue(order.pullDomainEvents().isEmpty());
    }

    @Test
    void failedPaymentRaisesNoEvent() {
        Order order = cancelledOrder();

        assertThrows(InvalidOrderTransitionException.class, order::recordPayment);

        assertTrue(order.pullDomainEvents().isEmpty());
    }

    @Test
    void failedCancellationRaisesNoEvent() {
        Order order = paidOrder();

        assertThrows(InvalidOrderTransitionException.class, order::cancel);

        assertTrue(order.pullDomainEvents().isEmpty());
    }

    @Test
    void eventsAreHandedOutOnlyOnce() {
        Order order = draftWithLine();
        order.confirm();

        assertEquals(1, order.pullDomainEvents().size());
        assertTrue(order.pullDomainEvents().isEmpty());
    }

    @Test
    void eventsFollowTheLifecycleInOrder() {
        Order order = draftWithLine();

        order.confirm();
        order.recordPayment();

        assertEquals(List.of(
                new OrderConfirmed(order.id(), usd("49.99")),
                new PaymentRecorded(order.id(), usd("49.99"))), order.pullDomainEvents());
    }

    @Test
    void ordersWithTheSameIdAreEqual() {
        OrderId id = OrderId.generate();

        assertEquals(Order.draft(id, USD), Order.draft(id, USD));
    }
}
