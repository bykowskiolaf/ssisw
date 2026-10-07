package dev.bykowski.ssisw.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bykowski.ssisw.car.Car;
import dev.bykowski.ssisw.market.Transaction;
import dev.bykowski.ssisw.simulation.MarketplaceEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "marketplace.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchMarketplaceStore implements MarketplaceEventSink {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchMarketplaceStore.class);

    private final ElasticsearchClient client;
    private final String carsIndex;
    private final String transactionsIndex;
    private final String runId = UUID.randomUUID().toString();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ElasticsearchMarketplaceStore(
            ElasticsearchClient client,
            @Value("${marketplace.elasticsearch.cars-index:cars}") String carsIndex,
            @Value("${marketplace.elasticsearch.transactions-index:transactions}") String transactionsIndex) {
        this.client = client;
        this.carsIndex = carsIndex;
        this.transactionsIndex = transactionsIndex;
    }

    public void indexCars(List<Car> cars) {
        try {
            ensureIndex(carsIndex, "elasticsearch/cars-index.json");
            ensureIndex(transactionsIndex, "elasticsearch/transactions-index.json");
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            cars.forEach(car -> bulk.operations(operation -> operation.index(index -> index
                    .index(carsIndex)
                    .id(documentId(car.id()))
                    .document(documentWithRunId(car)))));
            BulkResponse response = client.bulk(bulk.build());
            logBulkErrors(response);
            client.indices().refresh(refresh -> refresh.index(carsIndex));
            logger.info("Indexed {} cars in Elasticsearch index '{}' for run {}", cars.size(), carsIndex, runId);
        } catch (IOException | ElasticsearchException exception) {
            throw new IllegalStateException("Cannot initialize Elasticsearch marketplace indices", exception);
        }
    }

    @Override
    public void recordTransaction(Transaction transaction) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                // Both writes are idempotent, so retrying converges after a partial failure.
                client.index(index -> index
                        .index(transactionsIndex)
                        .id(documentId(transaction.id()))
                        .document(documentWithRunId(transaction)));
                client.update(update -> update
                                .index(carsIndex)
                                .id(documentId(transaction.carId()))
                                .doc(Map.of("status", "SOLD")),
                        Map.class);
                return;
            } catch (IOException | ElasticsearchException exception) {
                if (attempt == 3) {
                    throw new IllegalStateException("Cannot reconcile transaction " + transaction.id(), exception);
                }
                sleepBeforeRetry(attempt);
            }
        }
    }

    private void ensureIndex(String indexName, String mappingResource) throws IOException {
        if (client.indices().exists(exists -> exists.index(indexName)).value()) {
            client.indices().putMapping(mapping -> mapping
                    .index(indexName)
                    .properties("runId", property -> property.keyword(keyword -> keyword)));
            return;
        }
        ClassPathResource resource = new ClassPathResource(mappingResource);
        try (InputStream definition = resource.getInputStream()) {
            CreateIndexRequest request = CreateIndexRequest.of(create -> create
                    .index(indexName)
                    .withJson(definition));
            client.indices().create(request);
        }
    }

    public String runId() {
        return runId;
    }

    private String documentId(String entityId) {
        return runId + '-' + entityId;
    }

    private Map<String, Object> documentWithRunId(Object source) {
        Map<String, Object> document = new LinkedHashMap<>(
                objectMapper.convertValue(source, new TypeReference<>() { }));
        document.put("runId", runId);
        return document;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(100L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying Elasticsearch persistence", exception);
        }
    }

    private void logBulkErrors(BulkResponse response) {
        if (!response.errors()) {
            return;
        }
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                logger.error("Cannot index car {}: {}", item.id(), item.error().reason());
            }
        }
        throw new IllegalStateException("One or more cars could not be indexed");
    }
}
