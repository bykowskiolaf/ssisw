package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.market.Transaction;
import dev.bykowski.ssisw.market.ScoredOffer;
import dev.bykowski.ssisw.market.NegotiationResult;
import dev.bykowski.ssisw.market.MarketTrendSnapshot;
import dev.bykowski.ssisw.market.SellerReputation;
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
    private final AtomicInteger recommendationsGenerated = new AtomicInteger();
    private final AtomicInteger negotiationsAccepted = new AtomicInteger();
    private final AtomicInteger negotiationsRejected = new AtomicInteger();
    private final AtomicInteger ratingsRecorded = new AtomicInteger();
    private volatile MarketTrendSnapshot latestTrend;
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

    public void recommendationGenerated(String buyerName, List<ScoredOffer> recommendations) {
        recommendationsGenerated.incrementAndGet();
        String topCars = recommendations.stream()
                .map(recommendation -> "%s (%.3f)".formatted(
                        recommendation.offer().car().id(), recommendation.score()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        logger.info("Advisor TOP {} for {}: {}", recommendations.size(), buyerName, topCars);
    }

    public void negotiationCompleted(String buyerName, NegotiationResult result) {
        if (result.accepted()) {
            negotiationsAccepted.incrementAndGet();
            logger.info("NEGOTIATION: {} accepted {} PLN for {} after {} round(s), catalog {} PLN",
                    buyerName, result.agreedTotalPln(), result.carId(), result.rounds(), result.catalogTotalPln());
        } else {
            negotiationsRejected.incrementAndGet();
            logger.info("NEGOTIATION: {} rejected for {} after {} rounds; minimum {} PLN",
                    buyerName, result.carId(), result.rounds(), result.minimumTotalPln());
        }
    }

    public void ratingRecorded(
            String buyerName,
            String sellerName,
            int rating,
            SellerReputation reputation) {
        ratingsRecorded.incrementAndGet();
        logger.info("RATING: {} rated {} with {}/5; reputation {} from {} rating(s)",
                buyerName, sellerName, rating,
                "%.2f".formatted(reputation.averageRating()), reputation.ratingCount());
    }

    public void trendUpdated(MarketTrendSnapshot snapshot) {
        latestTrend = snapshot;
        logger.info("TREND: {} sales, popular body {}, model {}, fuel {}, average {} PLN, {}",
                snapshot.totalSales(), snapshot.mostPopularBodyType(), snapshot.mostPopularModel(),
                snapshot.mostPopularFuelType(),
                "%.2f".formatted(snapshot.averageTransactionPricePln()), snapshot.priceTrend());
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

    public boolean hasCompletedTransaction(String transactionId, String buyerName, String sellerName) {
        return transactions.stream().anyMatch(transaction ->
                transaction.id().equals(transactionId)
                        && transaction.buyerName().equals(buyerName)
                        && transaction.sellerName().equals(sellerName));
    }

    public boolean hasCompletedTransaction(Transaction candidate) {
        return transactions.contains(candidate);
    }

    public void simulationFailed(String agentName, String message) {
        completion.completeExceptionally(
                new IllegalStateException(agentName + ": " + message));
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

    public int recommendationsGenerated() {
        return recommendationsGenerated.get();
    }

    public int negotiationsAccepted() {
        return negotiationsAccepted.get();
    }

    public int negotiationsRejected() {
        return negotiationsRejected.get();
    }

    public int ratingsRecorded() {
        return ratingsRecorded.get();
    }

    public MarketTrendSnapshot latestTrend() {
        return latestTrend;
    }

    public List<Transaction> transactions() {
        return List.copyOf(transactions);
    }

    public Map<String, Long> remainingBudgets() {
        return Map.copyOf(remainingBudgets);
    }
}
