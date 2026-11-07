package dev.bykowski.ssisw.market;

public record MarketValuation(
        String carId,
        double marketAveragePln,
        long offerTotalPln,
        double percentageDifference,
        int comparableOffers,
        String classification
) {
}
