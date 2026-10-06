package dev.bykowski.ssisw.data;

import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarEnricherTests {

    private final CarEnricher enricher = new CarEnricher();

    @Test
    void createsDeterministicCompleteCar() {
        VpicModel model = new VpicModel("Toyota", "RAV4", "MULTIPURPOSE PASSENGER VEHICLE", 2024);

        Car first = enricher.enrich(model, 1);
        Car second = enricher.enrich(model, 1);

        assertThat(first).isEqualTo(second);
        assertThat(first.id()).isEqualTo("CAR-001");
        assertThat(first.bodyType()).isEqualTo("SUV");
        assertThat(first.totalCostPln()).isEqualTo(first.pricePln() + first.additionalCostPln());
        assertThat(first.status()).isEqualTo("AVAILABLE");
    }
}
