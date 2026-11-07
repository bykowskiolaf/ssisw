package dev.bykowski.ssisw.market;

import java.util.List;

public class ValuationService {

    public MarketValuation valuate(CarOffer target, List<CarOffer> marketOffers) {
        List<CarOffer> comparable = marketOffers.stream()
                .filter(offer -> !offer.car().id().equals(target.car().id()))
                .filter(offer -> offer.car().bodyType().equalsIgnoreCase(target.car().bodyType()))
                .filter(offer -> offer.car().fuelType().equalsIgnoreCase(target.car().fuelType()))
                .filter(offer -> Math.abs(offer.car().productionYear() - target.car().productionYear()) <= 2)
                .toList();
        if (comparable.isEmpty()) {
            comparable = marketOffers.stream()
                    .filter(offer -> !offer.car().id().equals(target.car().id()))
                    .toList();
        }
        double average = comparable.stream()
                .mapToLong(offer -> offer.car().totalCostPln())
                .average()
                .orElse(target.car().totalCostPln());
        double difference = average == 0.0
                ? 0.0
                : Math.abs(target.car().totalCostPln() - average) / average * 100.0;
        String classification = difference <= 5.0
                ? "CLOSE_TO_MARKET"
                : difference <= 15.0 ? "ELEVATED_OR_REDUCED" : "SIGNIFICANTLY_DIFFERENT";
        return new MarketValuation(target.car().id(), average, target.car().totalCostPln(),
                difference, comparable.size(), classification);
    }
}
