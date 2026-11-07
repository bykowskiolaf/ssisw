package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.market.Transaction;
import dev.bykowski.ssisw.market.ReturnResult;

@FunctionalInterface
public interface MarketplaceEventSink {

    void recordTransaction(Transaction transaction);

    default void recordReturn(ReturnResult result) {
    }

    static MarketplaceEventSink noop() {
        return transaction -> { };
    }
}
