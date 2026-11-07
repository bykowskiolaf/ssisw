package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValuationServiceTests {

    private final ValuationService service = new ValuationService();

    @Test
    void classifiesOfferAgainstComparableMarketAverage() {
        CarOffer target = offer("CAR-001", 110_000);
        List<CarOffer> market = List.of(target, offer("CAR-002", 100_000), offer("CAR-003", 100_000));

        MarketValuation valuation = service.valuate(target, market);

        assertThat(valuation.marketAveragePln()).isEqualTo(100_000.0);
        assertThat(valuation.percentageDifference()).isEqualTo(10.0);
        assertThat(valuation.comparableOffers()).isEqualTo(2);
        assertThat(valuation.classification()).isEqualTo("ELEVATED_OR_REDUCED");
    }

    @Test
    void usesBoundaryClassifications() {
        assertThat(service.valuate(offer("A", 105_000), List.of(offer("B", 100_000))).classification())
                .isEqualTo("CLOSE_TO_MARKET");
        assertThat(service.valuate(offer("A", 120_000), List.of(offer("B", 100_000))).classification())
                .isEqualTo("SIGNIFICANTLY_DIFFERENT");
    }

    private CarOffer offer(String id, long total) {
        return new CarOffer("Seller01", new Car(id, "Toyota", "RAV4", "SUV", "PETROL", 1998,
                2024, total - 2_000, 2_000, total, 10_000, 180, 7.0,
                "AUTOMATIC", "AWD", "AVAILABLE", "test"));
    }
}
