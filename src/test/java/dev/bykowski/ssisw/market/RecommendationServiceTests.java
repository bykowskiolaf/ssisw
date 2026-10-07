package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTests {

    private final RecommendationService recommendationService = new RecommendationService();

    @Test
    void returnsThreeHighestScoringEligibleOffers() {
        List<CarOffer> offers = List.of(
                offer("CAR-001", "SUV", 120_000),
                offer("CAR-002", "SUV", 130_000),
                offer("CAR-003", "SUV", 140_000),
                offer("CAR-004", "SUV", 150_000),
                offer("CAR-005", "SEDAN", 90_000));

        List<ScoredOffer> recommendations = recommendationService.recommendTop3(
                offers, new SearchCriteria("SUV", "PETROL", 2020, 200_000));

        assertThat(recommendations).hasSize(3);
        assertThat(recommendations).extracting(scored -> scored.offer().car().id())
                .containsExactly("CAR-001", "CAR-002", "CAR-003");
        assertThat(recommendations).allSatisfy(scored ->
                assertThat(scored.score()).isBetween(0.0, 1.0));
    }

    @Test
    void returnsNoRecommendationsWhenHardCriteriaRejectEveryOffer() {
        List<CarOffer> offers = List.of(offer("CAR-001", "SEDAN", 120_000));

        List<ScoredOffer> recommendations = recommendationService.recommendTop3(
                offers, new SearchCriteria("SUV", "ELECTRIC", 2025, 100_000));

        assertThat(recommendations).isEmpty();
    }

    @Test
    void fuelCriterionPrefersLowerEmissionTypeWhenFuelIsUnspecified() {
        CarOffer diesel = offer("CAR-001", "SUV", "DIESEL", 120_000);
        CarOffer petrol = offer("CAR-002", "SUV", "PETROL", 120_000);
        CarOffer hybrid = offer("CAR-003", "SUV", "HYBRID", 120_000);
        CarOffer electric = offer("CAR-004", "SUV", "ELECTRIC", 120_000);

        List<ScoredOffer> recommendations = recommendationService.recommendTop3(
                List.of(diesel, petrol, hybrid, electric),
                new SearchCriteria("SUV", null, 2020, 200_000));

        assertThat(recommendations).extracting(scored -> scored.offer().car().id())
                .containsExactly("CAR-004", "CAR-003", "CAR-002");
    }

    @Test
    void specifiedFuelTypeRemainsMandatory() {
        CarOffer petrol = offer("CAR-001", "SUV", "PETROL", 120_000);
        CarOffer electric = offer("CAR-002", "SUV", "ELECTRIC", 120_000);

        List<ScoredOffer> recommendations = recommendationService.recommendTop3(
                List.of(petrol, electric), new SearchCriteria("SUV", "PETROL", 2020, 200_000));

        assertThat(recommendations).extracting(scored -> scored.offer().car().id())
                .containsExactly("CAR-001");
    }

    private CarOffer offer(String id, String bodyType, long totalCost) {
        return offer(id, bodyType, "PETROL", totalCost);
    }

    private CarOffer offer(String id, String bodyType, String fuelType, long totalCost) {
        Car car = new Car(id, "Toyota", "RAV4", bodyType, fuelType, 1998,
                2024, totalCost - 2_000, 2_000, totalCost, 20_000, 180,
                7.0, "AUTOMATIC", "AWD", "AVAILABLE", "test");
        return new CarOffer("Seller01", car);
    }
}
