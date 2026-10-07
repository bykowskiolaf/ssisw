package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.market.Transaction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationObserverTests {

    @Test
    void forwardsCompletedTransactionToEventSink() {
        AtomicReference<Transaction> indexed = new AtomicReference<>();
        SimulationObserver observer = new SimulationObserver(1, indexed::set);
        Transaction transaction = new Transaction(
                "TX-1", "CAR-001", "Seller01", "Buyer01", 148_000, 1_700_000_000_000L);

        observer.purchaseCompleted(transaction, 152_000);
        observer.buyerCompleted("Buyer01", 152_000);

        assertThat(indexed).hasValue(transaction);
        assertThat(observer.transactions()).containsExactly(transaction);
        assertThat(observer.completion()).isCompleted();
    }

    @Test
    void surfacesPersistenceFailureThroughSimulationCompletion() {
        SimulationObserver observer = new SimulationObserver(1, transaction -> {
            throw new IllegalStateException("Elasticsearch unavailable");
        });
        Transaction transaction = new Transaction(
                "TX-1", "CAR-001", "Seller01", "Buyer01", 148_000, 1_700_000_000_000L);

        assertThatThrownBy(() -> observer.purchaseCompleted(transaction, 152_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Elasticsearch unavailable");
        assertThat(observer.completion()).isCompletedExceptionally();
        assertThat(observer.transactions()).isEmpty();
    }
}
