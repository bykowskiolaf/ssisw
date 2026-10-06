package dev.bykowski.ssisw.car;

public record Car(
        String id,
        String make,
        String model,
        String bodyType,
        String fuelType,
        int engineCapacityCc,
        int productionYear,
        long pricePln,
        long additionalCostPln,
        long totalCostPln,
        int mileageKm,
        int powerHp,
        double fuelConsumptionLitresPer100Km,
        String transmission,
        String drivetrain,
        String status,
        String dataSource
) {
}
