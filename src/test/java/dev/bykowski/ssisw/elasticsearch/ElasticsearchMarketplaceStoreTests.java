package dev.bykowski.ssisw.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import dev.bykowski.ssisw.car.Car;
import dev.bykowski.ssisw.market.ReturnResult;
import dev.bykowski.ssisw.market.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ELASTICSEARCH_INTEGRATION_TEST", matches = "true")
class ElasticsearchMarketplaceStoreTests {

    @Autowired
    private ElasticsearchClient client;

    @Test
    void delayedAndRepeatedReturnCannotOverwriteResale() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String carsIndex = "ssisw-test-cars-" + suffix;
        String transactionsIndex = "ssisw-test-transactions-" + suffix;
        ElasticsearchMarketplaceStore store = new ElasticsearchMarketplaceStore(
                client, carsIndex, transactionsIndex);
        try {
            Car car = new Car("CAR-001", "Toyota", "RAV4", "SUV", "PETROL", 1998,
                    2024, 120_000, 2_000, 122_000, 10_000, 180, 7.0,
                    "AUTOMATIC", "AWD", "AVAILABLE", "test");
            store.indexCars(List.of(car));
            Transaction first = new Transaction("TX-1", car.id(), "Seller01", "Buyer01", 110_000, 1000);
            Transaction resale = new Transaction("TX-2", car.id(), "Seller01", "Buyer02", 111_000, 2000);
            ReturnResult returned = new ReturnResult(true, first, 1500, "RETURNED");

            store.recordTransaction(first);
            store.recordReturn(returned);
            assertCarStatus(store, carsIndex, "AVAILABLE", first.id());

            store.recordTransaction(resale);
            // Reproduce a delayed return write (or retry) arriving after the replacement sale.
            store.recordReturn(returned);
            store.recordReturn(returned);

            assertCarStatus(store, carsIndex, "SOLD", resale.id());
            assertThat(client.get(get -> get.index(transactionsIndex)
                            .id(store.runId() + "-" + first.id()), Map.class).source())
                    .containsEntry("status", "RETURNED");
            assertThat(client.get(get -> get.index(transactionsIndex)
                            .id(store.runId() + "-" + resale.id()), Map.class).source())
                    .containsEntry("status", "COMPLETED");
        } finally {
            client.indices().delete(delete -> delete.index(carsIndex, transactionsIndex).ignoreUnavailable(true));
        }
    }

    private void assertCarStatus(
            ElasticsearchMarketplaceStore store, String index, String status, String transactionId) throws Exception {
        assertThat(client.get(get -> get.index(index).id(store.runId() + "-CAR-001"), Map.class).source())
                .containsEntry("status", status)
                .containsEntry("currentTransactionId", transactionId);
    }
}
