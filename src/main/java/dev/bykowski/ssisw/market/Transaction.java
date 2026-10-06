package dev.bykowski.ssisw.market;

public record Transaction(
        String id,
        String carId,
        String sellerName,
        String buyerName,
        long totalCostPln,
        long completedAtEpochMillis
) {
}
