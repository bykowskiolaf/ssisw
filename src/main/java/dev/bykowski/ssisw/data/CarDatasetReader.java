package dev.bykowski.ssisw.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bykowski.ssisw.car.Car;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CarDatasetReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Car> read(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Car dataset not found at %s. Generate it with "
                    .formatted(path.toAbsolutePath())
                    + "./gradlew bootRun --args='--cars.import.enabled=true'");
        }
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read car dataset from " + path.toAbsolutePath(), exception);
        }
    }
}
