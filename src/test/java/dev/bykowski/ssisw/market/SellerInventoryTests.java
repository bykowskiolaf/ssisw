package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SellerInventoryTests {

    @Test
    void preventsCompetingReservationAndDoubleSale() {
        MutableClock clock = new MutableClock();
        SellerInventory inventory = new SellerInventory("Seller01", List.of(car()), clock);

        assertThat(inventory.reserve("CAR-001", "Buyer01", 5_000).accepted()).isTrue();
        assertThat(inventory.reserve("CAR-001", "Buyer02", 5_000))
                .extracting(ReservationResult::accepted, ReservationResult::message)
                .containsExactly(false, "CAR_UNAVAILABLE");
        assertThat(inventory.purchase("CAR-001", "Buyer02")).isEmpty();

        Transaction transaction = inventory.purchase("CAR-001", "Buyer01").orElseThrow();

        assertThat(transaction.totalCostPln()).isEqualTo(148_000);
        assertThat(inventory.count(CarStatus.SOLD)).isOne();
        assertThat(inventory.purchase("CAR-001", "Buyer01")).isEmpty();
    }

    @Test
    void makesExpiredReservationAvailableAgain() {
        MutableClock clock = new MutableClock();
        SellerInventory inventory = new SellerInventory("Seller01", List.of(car()), clock);
        inventory.reserve("CAR-001", "Buyer01", 5_000);

        clock.advanceMillis(5_001);

        assertThat(inventory.count(CarStatus.AVAILABLE)).isOne();
        assertThat(inventory.reserve("CAR-001", "Buyer02", 5_000).accepted()).isTrue();
    }

    @Test
    void returnsCheapestMatchingTotalCost() {
        Car expensive = car("CAR-002", "SUV", "PETROL", 2024, 140_000, 10_000);
        Car cheaperTotal = car("CAR-003", "SUV", "PETROL", 2023, 145_000, 1_000);
        SellerInventory inventory = new SellerInventory("Seller01", List.of(expensive, cheaperTotal));

        CarOffer offer = inventory.findBestOffer(new SearchCriteria("SUV", "PETROL", 2020, 200_000))
                .orElseThrow();

        assertThat(offer.car().id()).isEqualTo("CAR-003");
        assertThat(offer.car().totalCostPln()).isEqualTo(146_000);
    }

    private Car car() {
        return car("CAR-001", "SUV", "PETROL", 2024, 145_000, 3_000);
    }

    private Car car(String id, String body, String fuel, int year, long price, long additionalCost) {
        return new Car(id, "Toyota", "RAV4", body, fuel, 1998, year, price, additionalCost,
                price + additionalCost, 20_000, 180, 7.2, "AUTOMATIC", "AWD", "AVAILABLE", "test");
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        private void advanceMillis(long milliseconds) {
            instant = instant.plusMillis(milliseconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
