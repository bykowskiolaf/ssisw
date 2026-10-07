package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.market.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationObserver {

    private static final Logger logger = LoggerFactory.getLogger(SimulationObserver.class);

    private final int expectedBuyers;
    private final MarketplaceEventSink eventSink;
    private final AtomicInteger offersProposed = new AtomicInteger();
    private final AtomicInteger rejectedReservations = new AtomicInteger();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final Set<String> completedBuyers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> remainingBudgets = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> completion = new CompletableFuture<>();

    public SimulationObserver(int expectedBuyers) {
        this(expectedBuyers, MarketplaceEventSink.noop());
    }

    public SimulationObserver(int expectedBuyers, MarketplaceEventSink eventSink) {
        this.expectedBuyers = expectedBuyers;
        this.eventSink = eventSink;
    }

    public void searchStarted(String buyerName, int sellerCount) {
        logger.info("{} requested offers from {} sellers", buyerName, sellerCount);
    }

    public void offerProposed(String sellerName, String buyerName, String carId, long totalCostPln) {
        offersProposed.incrementAndGet();
        logger.info("{} proposed {} to {} for {} PLN", sellerName, carId, buyerName, totalCostPln);
    }

    public void reservationRejected(String buyerName, String carId) {
        rejectedReservations.incrementAndGet();
        logger.info("{} could not reserve {}; searching again", buyerName, carId);
    }

    public void purchaseCompleted(Transaction transaction, long remainingBudgetPln) {
        try {
            eventSink.recordTransaction(transaction);
        } catch (RuntimeException exception) {
            completion.completeExceptionally(exception);
            throw exception;
        }
        transactions.add(transaction);
        logger.info("PURCHASE: {} bought {} from {} for {} PLN; remaining budget: {} PLN",
                transaction.buyerName(), transaction.carId(), transaction.sellerName(),
                transaction.totalCostPln(), remainingBudgetPln);
    }

    public void buyerCompleted(String buyerName, long remainingBudgetPln) {
        remainingBudgets.put(buyerName, remainingBudgetPln);
        completedBuyers.add(buyerName);
        if (completedBuyers.size() == expectedBuyers) {
            completion.complete(null);
        }
    }

    public CompletableFuture<Void> completion() {
        return completion;
    }

    public int offersProposed() {
        return offersProposed.get();
    }

    public int rejectedReservations() {
        return rejectedReservations.get();
    }

    public List<Transaction> transactions() {
        return List.copyOf(transactions);
    }

    public Map<String, Long> remainingBudgets() {
        return Map.copyOf(remainingBudgets);
    }
}
