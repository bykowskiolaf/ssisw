package dev.bykowski.ssisw.data;

import dev.bykowski.ssisw.car.Car;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CarEnricher {

    private static final String[] FUELS = {"PETROL", "DIESEL", "HYBRID", "ELECTRIC"};
    private static final int[] ENGINE_CAPACITIES = {1197, 1498, 1598, 1798, 1998, 2498};
    private static final String[] TRANSMISSIONS = {"MANUAL", "AUTOMATIC"};
    private static final String[] DRIVETRAINS = {"FWD", "RWD", "AWD"};

    public Car enrich(VpicModel source, int sequence) {
        int seed = Math.floorMod((source.make() + source.model() + source.productionYear()).hashCode(), Integer.MAX_VALUE);
        String fuel = FUELS[seed % FUELS.length];
        int engineCapacity = fuel.equals("ELECTRIC") ? 0 : ENGINE_CAPACITIES[(seed / 7) % ENGINE_CAPACITIES.length];
        String bodyType = bodyType(source.vehicleType(), seed);
        long price = roundToHundreds(55_000L + (long) (source.productionYear() - 2015) * 8_500L + seed % 65_000);
        long additionalCost = roundToHundreds(500L + seed % 5_500);
        int mileage = Math.max(500, (2026 - source.productionYear()) * 14_000 + seed % 12_000);
        int power = fuel.equals("ELECTRIC") ? 140 + seed % 220 : 90 + seed % 210;
        double consumption = fuel.equals("ELECTRIC") ? 0.0 : 4.5 + (seed % 46) / 10.0;

        return new Car(
                "CAR-%03d".formatted(sequence),
                source.make(),
                source.model(),
                bodyType,
                fuel,
                engineCapacity,
                source.productionYear(),
                price,
                additionalCost,
                price + additionalCost,
                mileage,
                power,
                consumption,
                TRANSMISSIONS[(seed / 11) % TRANSMISSIONS.length],
                DRIVETRAINS[(seed / 13) % DRIVETRAINS.length],
                "AVAILABLE",
                "NHTSA vPIC; market fields generated deterministically"
        );
    }

    private String bodyType(String vehicleType, int seed) {
        String normalized = vehicleType.toUpperCase(Locale.ROOT);
        if (normalized.contains("TRUCK")) {
            return "PICKUP";
        }
        if (normalized.contains("MULTIPURPOSE") || normalized.contains("SUV")) {
            return "SUV";
        }
        if (normalized.contains("BUS") || normalized.contains("VAN")) {
            return "VAN";
        }
        String[] passengerBodyTypes = {"SEDAN", "HATCHBACK", "WAGON", "COUPE"};
        return passengerBodyTypes[seed % passengerBodyTypes.length];
    }

    private long roundToHundreds(long value) {
        return Math.round(value / 100.0) * 100;
    }
}
