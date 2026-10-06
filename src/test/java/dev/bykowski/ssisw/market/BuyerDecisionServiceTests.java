package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuyerDecisionServiceTests {

    @Test
    void choosesCheapestAffordableOfferAndDeductsBudget() {
        BuyerAccount account = new BuyerAccount("Buyer01", 150_000);
        CarOffer tooExpensive = offer("CAR-001", 151_000);
        CarOffer affordable = offer("CAR-002", 140_000);

        CarOffer selected = new BuyerDecisionService()
                .selectBestAffordable(List.of(tooExpensive, affordable), account)
                .orElseThrow();
        account.pay(selected.car().totalCostPln());

        assertThat(selected.car().id()).isEqualTo("CAR-002");
        assertThat(account.budgetPln()).isEqualTo(10_000);
    }

    @Test
    void rejectsPaymentAboveBudget() {
        BuyerAccount account = new BuyerAccount("Buyer01", 100_000);

        assertThatThrownBy(() -> account.pay(100_001))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("budget");
        assertThat(account.budgetPln()).isEqualTo(100_000);
    }

    private CarOffer offer(String id, long totalCost) {
        Car car = new Car(id, "Toyota", "Corolla", "SEDAN", "PETROL", 1598,
                2024, totalCost - 1_000, 1_000, totalCost, 10_000, 130,
                6.5, "AUTOMATIC", "FWD", "AVAILABLE", "test");
        return new CarOffer("Seller01", car);
    }
}
