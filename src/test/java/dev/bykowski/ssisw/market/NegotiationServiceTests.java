package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NegotiationServiceTests {

    private final NegotiationService service = new NegotiationService();

    @Test
    void acceptsSellerCounterWithinBuyerMaximum() {
        CarOffer offer = new CarOffer("Seller01", car());

        NegotiationResult result = service.negotiate(offer, 119_000, 148_000);

        assertThat(result.accepted()).isTrue();
        assertThat(result.rounds()).isOne();
        assertThat(result.history()).hasSize(1);
        assertThat(result.history().getFirst().buyerOfferTotalPln()).isEqualTo(119_000);
        assertThat(result.history().getFirst().sellerCounterTotalPln()).isEqualTo(133_500);
        assertThat(result.agreedTotalPln()).isEqualTo(133_500);
        assertThat(result.minimumTotalPln()).isEqualTo(126_250);
    }

    @Test
    void rejectsAfterThreeRoundsWhenBuyerCannotReachMinimum() {
        CarOffer offer = new CarOffer("Seller01", car());

        NegotiationResult result = service.negotiate(offer, 110_000, 120_000);

        assertThat(result.accepted()).isFalse();
        assertThat(result.rounds()).isEqualTo(3);
        assertThat(result.history()).hasSize(3);
        assertThat(result.agreedTotalPln()).isZero();
    }

    private Car car() {
        return new Car("CAR-001", "Toyota", "RAV4", "SUV", "PETROL", 1998,
                2024, 145_000, 3_000, 148_000, 20_000, 180,
                7.2, "AUTOMATIC", "AWD", "AVAILABLE", "test");
    }
}
