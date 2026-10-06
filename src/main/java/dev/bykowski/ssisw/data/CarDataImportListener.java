package dev.bykowski.ssisw.data;

import dev.bykowski.ssisw.car.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "cars.import.enabled", havingValue = "true")
public class CarDataImportListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(CarDataImportListener.class);

    private final CarDatasetService datasetService;
    private final CarDatasetWriter datasetWriter;
    private final int count;
    private final int newestYear;
    private final Path outputPath;

    public CarDataImportListener(
            CarDatasetService datasetService,
            CarDatasetWriter datasetWriter,
            @Value("${cars.import.count:80}") int count,
            @Value("${cars.import.newest-year:2025}") int newestYear,
            @Value("${cars.import.output:data/cars.json}") String outputPath) {
        this.datasetService = datasetService;
        this.datasetWriter = datasetWriter;
        this.count = count;
        this.newestYear = newestYear;
        this.outputPath = Path.of(outputPath);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Car> cars = datasetService.createDataset(count, newestYear);
        datasetWriter.write(cars, outputPath);
        logger.info("Saved {} cleaned and enriched cars to {}", cars.size(), outputPath.toAbsolutePath());
        event.getApplicationContext().close();
    }
}
