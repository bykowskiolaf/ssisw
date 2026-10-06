package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;

public record SearchCriteria(String bodyType, String fuelType, int minimumYear, long maximumTotalCostPln) {

    public boolean matches(Car car) {
        return matchesOptional(bodyType, car.bodyType())
                && matchesOptional(fuelType, car.fuelType())
                && car.productionYear() >= minimumYear
                && car.totalCostPln() <= maximumTotalCostPln;
    }

    private boolean matchesOptional(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(actual);
    }
}
