package dev.bykowski.ssisw.data;

import dev.bykowski.ssisw.car.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CarDatasetService {

    private static final Logger logger = LoggerFactory.getLogger(CarDatasetService.class);
    private static final List<String> DEFAULT_MAKES = List.of(
            "Toyota", "Honda", "Ford", "BMW", "Mercedes-Benz",
            "Volkswagen", "Volvo", "Hyundai", "Kia", "Mazda");

    private final VpicClient vpicClient;
    private final CarEnricher enricher;

    public CarDatasetService(VpicClient vpicClient, CarEnricher enricher) {
        this.vpicClient = vpicClient;
        this.enricher = enricher;
    }

    public List<Car> createDataset(int requestedCount, int newestYear) {
        if (requestedCount < 1) {
            throw new IllegalArgumentException("Requested car count must be positive");
        }

        Map<String, VpicModel> uniqueModels = new LinkedHashMap<>();
        int recordsPerMake = (int) Math.ceil((double) requestedCount / DEFAULT_MAKES.size());

        for (int round = 0; uniqueModels.size() < requestedCount && round < 6; round++) {
            for (int makeIndex = 0; makeIndex < DEFAULT_MAKES.size() && uniqueModels.size() < requestedCount; makeIndex++) {
                String make = DEFAULT_MAKES.get(makeIndex);
                int year = newestYear - Math.floorMod(makeIndex + round, 6);
                try {
                    int before = uniqueModels.size();
                    for (VpicModel model : vpicClient.getModels(make, year)) {
                        String key = (model.make() + '|' + model.model() + '|' + model.productionYear())
                                .toLowerCase(Locale.ROOT);
                        uniqueModels.putIfAbsent(key, model);
                        if (uniqueModels.size() - before >= recordsPerMake || uniqueModels.size() >= requestedCount) {
                            break;
                        }
                    }
                } catch (RuntimeException exception) {
                    logger.warn("Skipping {} {} because its vPIC request failed: {}", make, year, exception.getMessage());
                }
            }
        }

        if (uniqueModels.size() < requestedCount) {
            throw new IllegalStateException("vPIC supplied only %d of %d required unique cars"
                    .formatted(uniqueModels.size(), requestedCount));
        }

        List<Car> cars = new ArrayList<>(requestedCount);
        int sequence = 1;
        for (VpicModel model : uniqueModels.values()) {
            cars.add(enricher.enrich(model, sequence++));
            if (cars.size() == requestedCount) {
                break;
            }
        }
        return List.copyOf(cars);
    }
}
