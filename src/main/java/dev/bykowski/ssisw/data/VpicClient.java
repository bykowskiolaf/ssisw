package dev.bykowski.ssisw.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class VpicClient {

    private static final List<String> VEHICLE_TYPES = List.of(
            "Passenger Car", "Multipurpose Passenger Vehicle", "Truck");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Autowired
    public VpicClient(@Value("${cars.import.vpic-base-url:https://vpic.nhtsa.dot.gov/api}") String baseUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), new ObjectMapper(), baseUrl);
    }

    VpicClient(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public List<VpicModel> getModels(String make, int year) {
        String encodedMake = URLEncoder.encode(make, StandardCharsets.UTF_8).replace("+", "%20");
        List<List<VpicModel>> modelsByType = VEHICLE_TYPES.stream()
                .map(vehicleType -> getModels(encodedMake, year, vehicleType))
                .toList();
        List<VpicModel> interleaved = new ArrayList<>();
        int longestList = modelsByType.stream().mapToInt(List::size).max().orElse(0);
        for (int modelIndex = 0; modelIndex < longestList; modelIndex++) {
            for (List<VpicModel> models : modelsByType) {
                if (modelIndex < models.size()) {
                    interleaved.add(models.get(modelIndex));
                }
            }
        }
        return List.copyOf(interleaved);
    }

    private List<VpicModel> getModels(String encodedMake, int year, String vehicleType) {
        String encodedVehicleType = URLEncoder.encode(vehicleType, StandardCharsets.UTF_8).replace("+", "%20");
        URI uri = URI.create(("%s/vehicles/GetModelsForMakeYear/make/%s/modelyear/%d/"
                + "vehicletype/%s?format=json").formatted(baseUrl, encodedMake, year, encodedVehicleType));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("vPIC returned HTTP " + response.statusCode());
            }
            VpicResponse payload = objectMapper.readValue(response.body(), VpicResponse.class);
            if (payload.results() == null) {
                return List.of();
            }
            return payload.results().stream()
                    .filter(result -> hasText(result.makeName()) && hasText(result.modelName()))
                    .map(result -> new VpicModel(
                            result.makeName().trim(),
                            result.modelName().trim(),
                            vehicleType,
                            year))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot parse the vPIC response", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("vPIC request was interrupted", exception);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VpicResponse(@JsonProperty("Results") List<VpicResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VpicResult(
            @JsonProperty("Make_Name") String makeName,
            @JsonProperty("Model_Name") String modelName
    ) {
    }
}
