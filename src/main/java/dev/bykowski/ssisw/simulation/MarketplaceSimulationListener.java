package dev.bykowski.ssisw.simulation;

import dev.bykowski.ssisw.agent.BuyerAgent;
import dev.bykowski.ssisw.agent.AdvisorAgent;
import dev.bykowski.ssisw.agent.NegotiatorAgent;
import dev.bykowski.ssisw.agent.ReputationAgent;
import dev.bykowski.ssisw.agent.TrendAgent;
import dev.bykowski.ssisw.agent.SellerAgent;
import dev.bykowski.ssisw.car.Car;
import dev.bykowski.ssisw.data.CarDatasetReader;
import dev.bykowski.ssisw.elasticsearch.ElasticsearchMarketplaceStore;
import dev.bykowski.ssisw.market.CarStatus;
import dev.bykowski.ssisw.market.SearchCriteria;
import dev.bykowski.ssisw.market.SellerInventory;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true")
public class MarketplaceSimulationListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceSimulationListener.class);
    private static final int SELLER_COUNT = 10;
    private static final int BUYER_COUNT = 5;
    private static final int CARS_PER_SELLER = 8;

    private final CarDatasetReader datasetReader;
    private final Path datasetPath;
    private final long buyerBudgetPln;
    private final int targetPurchasesPerBuyer;
    private final long reservationDurationMillis;
    private final Duration timeout;
    private final boolean jadeGui;
    private final int guiKeepOpenSeconds;
    private final DemoPacing demoPacing;
    private final boolean demonstrateReturn;
    private final long returnWindowMillis;
    private final ObjectProvider<ElasticsearchMarketplaceStore> elasticsearchStore;

    public MarketplaceSimulationListener(
            CarDatasetReader datasetReader,
            @Value("${simulation.dataset-path:data/cars.json}") String datasetPath,
            @Value("${simulation.buyer-budget-pln:500000}") long buyerBudgetPln,
            @Value("${simulation.target-purchases-per-buyer:3}") int targetPurchasesPerBuyer,
            @Value("${simulation.reservation-duration-ms:5000}") long reservationDurationMillis,
            @Value("${simulation.timeout-seconds:30}") long timeoutSeconds,
            @Value("${simulation.jade-gui:false}") boolean jadeGui,
            @Value("${simulation.gui-keep-open-seconds:60}") int guiKeepOpenSeconds,
            @Value("${simulation.demo-mode:false}") boolean demoMode,
            @Value("${simulation.demo-startup-delay-ms:15000}") long demoStartupDelayMillis,
            @Value("${simulation.demo-cycle-delay-ms:1000}") long demoCycleDelayMillis,
            @Value("${simulation.demonstrate-return:false}") boolean demonstrateReturn,
            @Value("${simulation.return-window-ms:5000}") long returnWindowMillis,
            ObjectProvider<ElasticsearchMarketplaceStore> elasticsearchStore) {
        this.datasetReader = datasetReader;
        this.datasetPath = Path.of(datasetPath);
        this.buyerBudgetPln = buyerBudgetPln;
        this.targetPurchasesPerBuyer = targetPurchasesPerBuyer;
        this.reservationDurationMillis = reservationDurationMillis;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.jadeGui = jadeGui;
        this.guiKeepOpenSeconds = guiKeepOpenSeconds;
        this.demoPacing = DemoPacing.forMode(demoMode, demoStartupDelayMillis, demoCycleDelayMillis);
        this.demonstrateReturn = demonstrateReturn;
        this.returnWindowMillis = returnWindowMillis;
        this.elasticsearchStore = elasticsearchStore;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Car> cars = datasetReader.read(datasetPath);
        int requiredCars = SELLER_COUNT * CARS_PER_SELLER;
        if (cars.size() < requiredCars) {
            throw new IllegalStateException("Simulation requires at least %d cars, but the dataset has %d"
                    .formatted(requiredCars, cars.size()));
        }
        if (targetPurchasesPerBuyer < 1) {
            throw new IllegalStateException("Target purchases per buyer must be positive");
        }
        if (guiKeepOpenSeconds < 0) {
            throw new IllegalStateException("GUI keep-open duration cannot be negative");
        }
        if (returnWindowMillis < 1) {
            throw new IllegalStateException("Return window must be positive");
        }

        ElasticsearchMarketplaceStore store = elasticsearchStore.getIfAvailable();
        if (store != null) {
            store.indexCars(cars.subList(0, requiredCars));
            logger.info("Elasticsearch simulation run ID: {}", store.runId());
        }
        SimulationObserver observer = new SimulationObserver(
                BUYER_COUNT, store == null ? MarketplaceEventSink.noop() : store);
        List<SellerInventory> inventories = createInventories(cars.subList(0, requiredCars));
        Runtime runtime = Runtime.instance();
        runtime.setCloseVM(true);
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, Boolean.toString(jadeGui));
        ContainerController container = runtime.createMainContainer(profile);

        startAgents(container, inventories, observer);
        logger.info("JADE marketplace started: {} sellers, {} buyers, {} cars",
                SELLER_COUNT, BUYER_COUNT, requiredCars);
        if (demoPacing.startupDelayMillis() > 0 || demoPacing.cycleDelayMillis() > 0) {
            logger.info("Slow demo: {} ms startup pause to attach Sniffer, {} ms between buyer search cycles",
                    demoPacing.startupDelayMillis(), demoPacing.cycleDelayMillis());
        }

        Thread.ofVirtual().name("simulation-completion").start(() -> {
            try {
                observer.completion().get(
                        timeout.plusMillis(demoPacing.startupDelayMillis()).toMillis(), TimeUnit.MILLISECONDS);
                printSummary(inventories, observer);
                if (jadeGui && guiKeepOpenSeconds > 0) {
                    logger.info("Simulation complete. Keeping JADE GUI open for {} seconds; Ctrl+C stops it sooner.",
                            guiKeepOpenSeconds);
                    TimeUnit.SECONDS.sleep(guiKeepOpenSeconds);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.info("Simulation completion wait interrupted; shutting down JADE");
            } catch (Exception exception) {
                logger.error("Simulation failed or did not complete within {} seconds", timeout.toSeconds(), exception);
            } finally {
                try {
                    container.kill();
                } catch (StaleProxyException exception) {
                    logger.warn("JADE container was already stopped: {}", exception.getMessage());
                }
                runtime.shutDown();
                event.getApplicationContext().close();
            }
        });
    }

    private List<SellerInventory> createInventories(List<Car> cars) {
        List<SellerInventory> inventories = new ArrayList<>(SELLER_COUNT);
        for (int sellerIndex = 0; sellerIndex < SELLER_COUNT; sellerIndex++) {
            int from = sellerIndex * CARS_PER_SELLER;
            String sellerName = "Seller%02d".formatted(sellerIndex + 1);
            inventories.add(new SellerInventory(sellerName, cars.subList(from, from + CARS_PER_SELLER)));
        }
        return List.copyOf(inventories);
    }

    private void startAgents(
            ContainerController container,
            List<SellerInventory> inventories,
            SimulationObserver observer) {
        try {
            for (SellerInventory inventory : inventories) {
                String sellerName = "Seller%02d".formatted(inventories.indexOf(inventory) + 1);
                container.createNewAgent(
                        sellerName,
                        SellerAgent.class.getName(),
                        new Object[]{inventory, observer, reservationDurationMillis, returnWindowMillis})
                        .start();
            }
            container.createNewAgent(
                    "Advisor",
                    AdvisorAgent.class.getName(),
                    new Object[]{observer})
                    .start();
            container.createNewAgent(
                    "Negotiator",
                    NegotiatorAgent.class.getName(),
                    new Object[]{observer})
                    .start();
            container.createNewAgent(
                    "Reputation",
                    ReputationAgent.class.getName(),
                    new Object[]{observer})
                    .start();
            container.createNewAgent(
                    "Trend",
                    TrendAgent.class.getName(),
                    new Object[]{observer})
                    .start();
            for (int buyerIndex = 0; buyerIndex < BUYER_COUNT; buyerIndex++) {
                String buyerName = "Buyer%02d".formatted(buyerIndex + 1);
                SearchCriteria criteria = new SearchCriteria(null, null, 2020, buyerBudgetPln);
                container.createNewAgent(
                        buyerName,
                        BuyerAgent.class.getName(),
                        new Object[]{buyerBudgetPln, criteria, observer, SELLER_COUNT,
                                 targetPurchasesPerBuyer, demonstrateReturn, demoPacing})
                        .start();
            }
        } catch (StaleProxyException exception) {
            throw new IllegalStateException("Cannot start JADE marketplace agents", exception);
        }
    }

    private void printSummary(List<SellerInventory> inventories, SimulationObserver observer) {
        long available = inventories.stream().mapToLong(inventory -> inventory.count(CarStatus.AVAILABLE)).sum();
        long reserved = inventories.stream().mapToLong(inventory -> inventory.count(CarStatus.RESERVED)).sum();
        long sold = inventories.stream().mapToLong(inventory -> inventory.count(CarStatus.SOLD)).sum();

        logger.info("================ SIMULATION SUMMARY ================");
        logger.info("Offers proposed: {}", observer.offersProposed());
        logger.info("TOP 3 recommendations generated: {}", observer.recommendationsGenerated());
        logger.info("Negotiations: {} accepted, {} rejected",
                observer.negotiationsAccepted(), observer.negotiationsRejected());
        logger.info("Seller ratings recorded: {}", observer.ratingsRecorded());
        logger.info("Market valuations generated: {}", observer.valuationsGenerated());
        logger.info("Returns completed: {}", observer.returns().size());
        if (observer.latestTrend() != null) {
            logger.info("Market result: popular body {}, model {}, fuel {}; average {} PLN; trend {}",
                    observer.latestTrend().mostPopularBodyType(), observer.latestTrend().mostPopularModel(),
                    observer.latestTrend().mostPopularFuelType(),
                    Math.round(observer.latestTrend().averageTransactionPricePln()),
                    observer.latestTrend().priceTrend());
        }
        logger.info("Rejected competing reservations: {}", observer.rejectedReservations());
        logger.info("Purchases completed: {}", observer.transactions().size());
        logger.info("Cars: {} available, {} reserved, {} sold", available, reserved, sold);
        new TreeMap<>(observer.remainingBudgets()).forEach((buyer, budget) ->
                logger.info("{} remaining budget: {} PLN", buyer, budget));
        logger.info("====================================================");
    }
}
