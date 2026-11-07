package dev.bykowski.ssisw.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrendService {

    private final List<SaleRecord> sales = new ArrayList<>();

    public synchronized MarketTrendSnapshot record(SaleRecord sale) {
        sales.add(sale);
        return snapshot();
    }

    public synchronized MarketTrendSnapshot snapshot() {
        if (sales.isEmpty()) {
            return new MarketTrendSnapshot(0, "N/A", "N/A", "N/A", 0.0, "STABLE");
        }
        return new MarketTrendSnapshot(
                sales.size(),
                mostPopular(sale -> sale.car().bodyType()),
                mostPopular(sale -> sale.car().make() + " " + sale.car().model()),
                mostPopular(sale -> sale.car().fuelType()),
                sales.stream().mapToLong(sale -> sale.transaction().totalCostPln()).average().orElse(0.0),
                priceTrend());
    }

    private String mostPopular(Function<SaleRecord, String> classifier) {
        return sales.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("N/A");
    }

    private String priceTrend() {
        if (sales.size() < 4) {
            return "STABLE";
        }
        int midpoint = sales.size() / 2;
        double older = average(sales.subList(0, midpoint));
        double newer = average(sales.subList(midpoint, sales.size()));
        double change = (newer - older) / older;
        if (change > 0.02) {
            return "RISING";
        }
        if (change < -0.02) {
            return "FALLING";
        }
        return "STABLE";
    }

    private double average(List<SaleRecord> records) {
        return records.stream().mapToLong(record -> record.transaction().totalCostPln()).average().orElse(0.0);
    }
}
