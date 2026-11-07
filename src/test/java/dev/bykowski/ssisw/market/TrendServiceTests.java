package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrendServiceTests {

    @Test
    void aggregatesPopularCategoriesAndDetectsRisingPrices() {
        TrendService service = new TrendService();
        service.record(sale("TX-1", "SUV", "PETROL", 100_000));
        service.record(sale("TX-2", "SUV", "PETROL", 105_000));
        service.record(sale("TX-3", "SUV", "HYBRID", 130_000));
        MarketTrendSnapshot snapshot = service.record(sale("TX-4", "SEDAN", "PETROL", 140_000));

        assertThat(snapshot.totalSales()).isEqualTo(4);
        assertThat(snapshot.mostPopularBodyType()).isEqualTo("SUV");
        assertThat(snapshot.mostPopularModel()).isEqualTo("Toyota RAV4");
        assertThat(snapshot.mostPopularFuelType()).isEqualTo("PETROL");
        assertThat(snapshot.averageTransactionPricePln()).isEqualTo(118_750.0);
        assertThat(snapshot.priceTrend()).isEqualTo("RISING");
    }

    private SaleRecord sale(String transactionId, String body, String fuel, long price) {
        Car car = new Car("CAR-" + transactionId, "Toyota", "RAV4", body, fuel, 1998,
                2024, price, 0, price, 10_000, 180, 7.0,
                "AUTOMATIC", "AWD", "SOLD", "test");
        Transaction transaction = new Transaction(
                transactionId, car.id(), "Seller01", "Buyer01", price, 1_000);
        return new SaleRecord(transaction, car);
    }
}
