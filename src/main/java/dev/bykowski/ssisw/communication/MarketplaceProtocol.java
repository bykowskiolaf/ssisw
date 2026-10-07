package dev.bykowski.ssisw.communication;

import dev.bykowski.ssisw.market.CarOffer;
import dev.bykowski.ssisw.market.ReservationResult;
import dev.bykowski.ssisw.market.SearchCriteria;
import dev.bykowski.ssisw.market.ScoredOffer;
import dev.bykowski.ssisw.market.Transaction;

public final class MarketplaceProtocol {

    public static final String SELLER_SERVICE = "car-seller";
    public static final String SEARCH = "car-search";
    public static final String RESERVATION = "car-reservation";
    public static final String PURCHASE = "car-purchase";
    public static final String ADVISOR_SERVICE = "car-advisor";
    public static final String RECOMMENDATION = "car-recommendation";

    private MarketplaceProtocol() {
    }

    public record SearchRequest(SearchCriteria criteria) {
    }

    public record SearchResponse(CarOffer offer) {
    }

    public record ReservationRequest(String carId, String buyerName) {
    }

    public record ReservationResponse(ReservationResult result) {
    }

    public record PurchaseRequest(String carId, String buyerName) {
    }

    public record PurchaseResponse(Transaction transaction) {
    }

    public record RecommendationRequest(java.util.List<CarOffer> offers, SearchCriteria criteria) {
    }

    public record RecommendationResponse(java.util.List<ScoredOffer> recommendations) {
    }
}
