package dev.bykowski.ssisw.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bykowski.ssisw.car.Car;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarDatasetWriterTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesElasticsearchReadyJsonArray() throws Exception {
        Car car = new Car("CAR-001", "Toyota", "RAV4", "SUV", "PETROL", 1998,
                2024, 145_000, 3_000, 148_000, 20_000, 180, 7.2,
                "AUTOMATIC", "AWD", "AVAILABLE", "test");
        Path output = temporaryDirectory.resolve("nested/cars.json");

        new CarDatasetWriter().write(List.of(car), output);

        JsonNode json = new ObjectMapper().readTree(output.toFile());
        assertThat(json).hasSize(1);
        assertThat(json.get(0).get("totalCostPln").asLong()).isEqualTo(148_000);
    }
}
