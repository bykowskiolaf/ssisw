package dev.bykowski.ssisw.communication;

import dev.bykowski.ssisw.market.CarOffer;
import dev.bykowski.ssisw.market.ReservationResult;
import dev.bykowski.ssisw.market.SearchCriteria;
import dev.bykowski.ssisw.market.ScoredOffer;
import dev.bykowski.ssisw.market.NegotiationResult;
import dev.bykowski.ssisw.market.MarketTrendSnapshot;
import dev.bykowski.ssisw.market.SaleRecord;
import dev.bykowski.ssisw.market.SellerReputation;
import dev.bykowski.ssisw.market.Transaction;

public final class MarketplaceProtocol {

    public static final String SELLER_SERVICE = "car-seller";
    public static final String SEARCH = "car-search";
    public static final String RESERVATION = "car-reservation";
    public static final String PURCHASE = "car-purchase";
    public static final String ADVISOR_SERVICE = "car-advisor";
    public static final String RECOMMENDATION = "car-recommendation";
    public static final String NEGOTIATOR_SERVICE = "car-negotiator";
    public static final String NEGOTIATION = "car-negotiation";
    public static final String REPUTATION_SERVICE = "seller-reputation";
    public static final String REPUTATION_QUERY = "reputation-query";
    public static final String RATING = "seller-rating";
    public static final String TREND_SERVICE = "market-trend";
    public static final String SALE_EVENT = "market-sale-event";

    private MarketplaceProtocol() {
    }

    public record SearchRequest(SearchCriteria criteria) {
    }

    public record SearchResponse(CarOffer offer) {
    }

    public record ReservationRequest(String carId, String buyerName, long agreedTotalPln) {
    }

    public record ReservationResponse(ReservationResult result) {
    }

    public record PurchaseRequest(String carId, String buyerName) {
    }

    public record PurchaseResponse(Transaction transaction) {
    }

    public record RecommendationRequest(
            java.util.List<CarOffer> offers,
            SearchCriteria criteria,
            java.util.Map<String, Double> sellerReputations) {
    }

    public record RecommendationResponse(java.util.List<ScoredOffer> recommendations) {
    }

    public record NegotiationRequest(
            CarOffer offer,
            String buyerName,
            long initialOfferTotalPln,
            long buyerMaximumTotalPln
    ) {
    }

    public record NegotiationResponse(NegotiationResult result) {
    }

    public record ReputationQuery(java.util.List<String> sellerNames) {
    }

    public record ReputationResponse(java.util.Map<String, SellerReputation> reputations) {
    }

    public record RatingRequest(
            String transactionId,
            String sellerName,
            String buyerName,
            int rating
    ) {
    }

    public record RatingResponse(SellerReputation reputation) {
    }

    public record SaleEventRequest(String buyerName, SaleRecord sale) {
    }

    public record SaleEventResponse(MarketTrendSnapshot snapshot) {
    }
}
