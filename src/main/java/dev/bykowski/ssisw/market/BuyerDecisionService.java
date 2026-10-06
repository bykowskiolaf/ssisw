package dev.bykowski.ssisw.market;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BuyerDecisionService {

    public Optional<CarOffer> selectBestAffordable(List<CarOffer> offers, BuyerAccount account) {
        return offers.stream()
                .filter(offer -> account.canAfford(offer.car().totalCostPln()))
                .min(Comparator.comparingLong((CarOffer offer) -> offer.car().totalCostPln())
                        .thenComparing(offer -> offer.car().id()));
    }
}
