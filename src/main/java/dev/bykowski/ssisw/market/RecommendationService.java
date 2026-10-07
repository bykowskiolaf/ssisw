package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class RecommendationService {

    private static final double PRICE_WEIGHT = 0.30;
    private static final double YEAR_WEIGHT = 0.20;
    private static final double TOTAL_COST_WEIGHT = 0.15;
    private static final double FUEL_WEIGHT = 0.10;
    private static final double ENGINE_CAPACITY_WEIGHT = 0.10;
    private static final double CONSUMPTION_WEIGHT = 0.15;

    public List<ScoredOffer> recommendTop3(List<CarOffer> offers, SearchCriteria criteria) {
        List<CarOffer> eligible = offers.stream()
                .filter(offer -> criteria.matches(offer.car()))
                .toList();
        if (eligible.isEmpty()) {
            return List.of();
        }

        Range price = range(eligible, offer -> offer.car().pricePln());
        Range year = range(eligible, offer -> offer.car().productionYear());
        Range totalCost = range(eligible, offer -> offer.car().totalCostPln());
        Range capacity = range(eligible, offer -> offer.car().engineCapacityCc());
        Range consumption = range(eligible, offer -> offer.car().fuelConsumptionLitresPer100Km());

        return eligible.stream()
                .map(offer -> new ScoredOffer(offer,
                        lowerIsBetter(offer.car().pricePln(), price) * PRICE_WEIGHT
                                + higherIsBetter(offer.car().productionYear(), year) * YEAR_WEIGHT
                                + lowerIsBetter(offer.car().totalCostPln(), totalCost) * TOTAL_COST_WEIGHT
                                + fuelScore(offer.car(), criteria) * FUEL_WEIGHT
                                + higherIsBetter(offer.car().engineCapacityCc(), capacity) * ENGINE_CAPACITY_WEIGHT
                                + lowerIsBetter(offer.car().fuelConsumptionLitresPer100Km(), consumption)
                                * CONSUMPTION_WEIGHT))
                .sorted(Comparator.comparingDouble(ScoredOffer::score).reversed()
                        .thenComparingLong(scored -> scored.offer().car().totalCostPln())
                        .thenComparing(scored -> scored.offer().car().id()))
                .limit(3)
                .toList();
    }

    private double fuelScore(Car car, SearchCriteria criteria) {
        if (criteria.fuelType() != null && !criteria.fuelType().isBlank()) {
            return criteria.fuelType().equalsIgnoreCase(car.fuelType()) ? 1.0 : 0.0;
        }
        return switch (car.fuelType().toUpperCase()) {
            case "ELECTRIC" -> 1.0;
            case "HYBRID" -> 0.8;
            case "PETROL" -> 0.6;
            case "DIESEL" -> 0.4;
            default -> 0.2;
        };
    }

    private Range range(List<CarOffer> offers, ToDoubleFunction<CarOffer> extractor) {
        double minimum = offers.stream().mapToDouble(extractor).min().orElseThrow();
        double maximum = offers.stream().mapToDouble(extractor).max().orElseThrow();
        return new Range(minimum, maximum);
    }

    private double lowerIsBetter(double value, Range range) {
        return range.isConstant() ? 1.0 : (range.maximum() - value) / (range.maximum() - range.minimum());
    }

    private double higherIsBetter(double value, Range range) {
        return range.isConstant() ? 1.0 : (value - range.minimum()) / (range.maximum() - range.minimum());
    }

    private record Range(double minimum, double maximum) {
        private boolean isConstant() {
            return Double.compare(minimum, maximum) == 0;
        }
    }
}
