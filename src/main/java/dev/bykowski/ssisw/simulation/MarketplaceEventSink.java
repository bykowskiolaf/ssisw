package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.market.Transaction;

@FunctionalInterface
public interface MarketplaceEventSink {

    void recordTransaction(Transaction transaction);

    static MarketplaceEventSink noop() {
        return transaction -> { };
    }
}
