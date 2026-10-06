package dev.bykowski.ssisw.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.bykowski.ssisw.car.Car;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CarDatasetWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(List<Car> cars, Path outputPath) {
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(outputPath.toFile(), cars);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write car dataset to " + outputPath, exception);
        }
    }
}
