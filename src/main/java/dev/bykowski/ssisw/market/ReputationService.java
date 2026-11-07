package dev.bykowski.ssisw.market;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReputationService {

    private final Map<String, RatingAggregate> ratings = new HashMap<>();
    private final Set<String> ratedTransactions = new HashSet<>();

    public synchronized SellerReputation rate(
            String transactionId,
            String sellerName,
            int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (!ratedTransactions.add(transactionId)) {
            throw new IllegalStateException("Transaction has already been rated");
        }
        RatingAggregate aggregate = ratings.computeIfAbsent(sellerName, ignored -> new RatingAggregate());
        aggregate.sum += rating;
        aggregate.count++;
        return reputation(sellerName);
    }

    public synchronized Map<String, SellerReputation> reputations(List<String> sellerNames) {
        Map<String, SellerReputation> result = new HashMap<>();
        sellerNames.forEach(name -> result.put(name, reputation(name)));
        return Map.copyOf(result);
    }

    private SellerReputation reputation(String sellerName) {
        RatingAggregate aggregate = ratings.get(sellerName);
        if (aggregate == null) {
            return new SellerReputation(sellerName, 0.0, 0);
        }
        return new SellerReputation(sellerName, (double) aggregate.sum / aggregate.count, aggregate.count);
    }

    private static final class RatingAggregate {
        private int sum;
        private int count;
    }
}
