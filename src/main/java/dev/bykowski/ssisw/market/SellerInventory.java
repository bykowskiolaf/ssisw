package dev.bykowski.ssisw.market;

import dev.bykowski.ssisw.car.Car;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SellerInventory {

    private final String sellerName;
    private final Clock clock;
    private final Map<String, InventoryItem> items = new LinkedHashMap<>();

    public SellerInventory(String sellerName, List<Car> cars) {
        this(sellerName, cars, Clock.systemUTC());
    }

    SellerInventory(String sellerName, List<Car> cars, Clock clock) {
        this.sellerName = sellerName;
        this.clock = clock;
        cars.forEach(car -> items.put(car.id(), new InventoryItem(car)));
    }

    public synchronized Optional<CarOffer> findBestOffer(SearchCriteria criteria) {
        releaseExpiredReservations();
        return items.values().stream()
                .filter(item -> item.status == CarStatus.AVAILABLE)
                .map(item -> item.car)
                .filter(criteria::matches)
                .min(Comparator.comparingLong(Car::totalCostPln).thenComparing(Car::id))
                .map(car -> new CarOffer(sellerName, car));
    }

    public synchronized ReservationResult reserve(String carId, String buyerName, long durationMillis) {
        releaseExpiredReservations();
        InventoryItem item = items.get(carId);
        if (item == null) {
            return new ReservationResult(false, carId, "UNKNOWN_CAR");
        }
        if (item.status != CarStatus.AVAILABLE) {
            return new ReservationResult(false, carId, "CAR_UNAVAILABLE");
        }
        item.status = CarStatus.RESERVED;
        item.reservedBy = buyerName;
        item.reservedUntilEpochMillis = clock.millis() + durationMillis;
        return new ReservationResult(true, carId, "RESERVED");
    }

    public synchronized Optional<Transaction> purchase(String carId, String buyerName) {
        releaseExpiredReservations();
        InventoryItem item = items.get(carId);
        if (item == null || item.status != CarStatus.RESERVED || !buyerName.equals(item.reservedBy)) {
            return Optional.empty();
        }
        item.status = CarStatus.SOLD;
        item.reservedBy = null;
        item.reservedUntilEpochMillis = 0;
        return Optional.of(new Transaction(
                UUID.randomUUID().toString(),
                carId,
                sellerName,
                buyerName,
                item.car.totalCostPln(),
                clock.millis()));
    }

    public synchronized void releaseExpiredReservations() {
        long now = clock.millis();
        items.values().stream()
                .filter(item -> item.status == CarStatus.RESERVED && item.reservedUntilEpochMillis <= now)
                .forEach(item -> {
                    item.status = CarStatus.AVAILABLE;
                    item.reservedBy = null;
                    item.reservedUntilEpochMillis = 0;
                });
    }

    public synchronized long count(CarStatus status) {
        releaseExpiredReservations();
        return items.values().stream().filter(item -> item.status == status).count();
    }

    public int size() {
        return items.size();
    }

    private static final class InventoryItem {
        private final Car car;
        private CarStatus status = CarStatus.AVAILABLE;
        private String reservedBy;
        private long reservedUntilEpochMillis;

        private InventoryItem(Car car) {
            this.car = car;
        }
    }
}
