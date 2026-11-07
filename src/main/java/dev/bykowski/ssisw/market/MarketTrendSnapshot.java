package dev.bykowski.ssisw.market;

public record MarketTrendSnapshot(
        int totalSales,
        String mostPopularBodyType,
        String mostPopularModel,
        String mostPopularFuelType,
        double averageTransactionPricePln,
        String priceTrend
) {
}
