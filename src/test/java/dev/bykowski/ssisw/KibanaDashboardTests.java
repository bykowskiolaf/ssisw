package dev.bykowski.ssisw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KibanaDashboardTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dashboardExportContainsValidSavedObjectsAndVegaSpecifications() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("kibana/dashboard.ndjson"));

        assertThat(lines).hasSize(8);
        int visualizations = 0;
        for (String line : lines) {
            JsonNode savedObject = objectMapper.readTree(line);
            assertThat(savedObject.path("type").asText()).isNotBlank();
            assertThat(savedObject.path("id").asText()).isNotBlank();

            if (savedObject.path("type").asText().equals("visualization")) {
                visualizations++;
                JsonNode visState = objectMapper.readTree(
                        savedObject.path("attributes").path("visState").asText());
                objectMapper.readTree(visState.path("params").path("spec").asText());
            }
        }
        assertThat(visualizations).isEqualTo(5);
    }
}
