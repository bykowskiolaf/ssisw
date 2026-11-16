# SSISW car market

Multi-agent car-market project built with Java, Spring Boot, JADE, Elasticsearch, and Kibana.

Project documentation:

- [final report and architecture](docs/SPRAWOZDANIE.md),
- [17 test scenarios and results](docs/SCENARIUSZE_TESTOWE.md),
- [presentation checklist](docs/DEMO.md),
- [importable Kibana dashboard](kibana/README.md).

## Generate the vehicle dataset

The importer retrieves real make, model, production-year, and vehicle-category data from the public
[NHTSA vPIC API](https://vpic.nhtsa.dot.gov/api/). vPIC does not provide market offers, so price,
fees, mileage, engine details, and other market attributes are generated deterministically. The
same API record always receives the same enriched values.

Generate the default 80 records:

```shell
./gradlew bootRun --args='--cars.import.enabled=true'
```

The resulting file is `data/cars.json`. Override defaults when needed:

```shell
./gradlew bootRun --args='--cars.import.enabled=true --cars.import.count=100 --cars.import.newest-year=2024 --cars.import.output=data/cars-2024.json'
```

The import rejects incomplete records, removes duplicate make/model/year combinations, tolerates
individual failed API requests, and only writes the file when it obtains the requested number of
vehicles. Generated files are ignored by Git because they can be reproduced from the API.

## Run the first JADE marketplace simulation

Generate the dataset once, then start the simulation:

```shell
./gradlew bootRun --args='--simulation.enabled=true'
```

The simulation starts 10 seller agents, 5 buyer agents, and allocates 8 cars to every seller.
Buyers discover sellers through the JADE Directory Facilitator, request offers with ACL messages,
send eligible offers to an independent advisor, reserve the highest-ranked car, and complete a
purchase. The advisor returns TOP 3 using normalized MCDA weights: price 30%, production year 20%,
total cost 15%, fuel type 10%, engine capacity 10%, and fuel consumption 15%. Broad shared preferences
intentionally create competition; buyers whose reservation is rejected search again. The process
continues until every buyer has purchased three cars, then prints a 15-transaction summary and
shuts down automatically.

When no fuel type is explicitly requested, the fuel criterion prefers electric, hybrid, petrol,
then diesel vehicles. When a fuel type is requested, it remains a mandatory filter.

Before reservation, the selected offer is sent to an independent `NegotiatorAgent`. Buyers begin
at 80% of catalog price, sellers counter while enforcing a maximum 15% discount, and negotiation
ends with acceptance or rejection after at most three rounds. Additional costs are not discounted.
The seller independently validates the agreed total, and that amount is used for the transaction
and buyer budget deduction.

After every purchase, the buyer sends one rating from 1 to 5 to `ReputationAgent`. Duplicate
transaction ratings are rejected, averages are recalculated, and seller reputation is used as a
tie-breaker by the advisor. `TrendAgent` receives each completed sale and reports the most popular
body type, model and fuel type, average transaction price, and a rising/falling/stable price trend.

The advisor also estimates the selected car's market value from offers with a similar body type,
fuel type, and production year. Price differences are classified as close to market (up to 5%),
elevated/reduced (5–15%), or significantly different (above 15%).

Returns are accepted within a configurable five-second window. The seller restores the car to
`AVAILABLE`, the buyer receives a full refund, and Elasticsearch marks the transaction `RETURNED`.
Run the demonstration scenario with:

```shell
./gradlew bootRun --args='--simulation.enabled=true --simulation.demonstrate-return=true'
```

`Buyer01` returns its first purchase and buys a replacement, so the run ends with 16 purchase
records, one return, and the required 15 sold cars.

Enable JADE's management GUI when inspecting agents and messages manually:

```shell
./gradlew bootRun --args='--simulation.enabled=true --simulation.jade-gui=true --simulation.timeout-seconds=120'
```

The launcher disables Spring Boot's default headless mode before startup for JADE's Swing GUI.
Run in a graphical desktop session and do not force `-Djava.awt.headless=true`.
The timeout is a maximum execution limit, not a GUI display duration. After a successful
GUI run, the platform stays open for another 60 seconds. Override this with
`--simulation.gui-keep-open-seconds=300` for five minutes, or `=0` for immediate shutdown.
Console-only runs still exit immediately. Ctrl+C stops the application sooner.
Completed buyer agents remove themselves, so the hold period preserves the remaining
platform and GUI, not the full initial buyer list.

For a slower Sniffer demonstration, add `--simulation.demo-mode=true`: buyers wait
15 seconds before trading, then pause for 1 second between search cycles (including
retries). Override with `--simulation.demo-startup-delay-ms=30000` and
`--simulation.demo-cycle-delay-ms=2000`. These pauses use nonblocking behaviour
waits and do not delay an active reservation, purchase, or return. The startup pause
is added to the simulation timeout; for long cycle delays, increase
`--simulation.timeout-seconds=120`. Normal mode has no demo delays.

The simulation currently uses `data/cars.json`. If it is absent, run the importer first. Useful
settings are available under `simulation.*` in `application.properties`.

## Elasticsearch and Kibana

Start the local single-node Elastic stack:

```shell
docker compose up -d
```

Elasticsearch is available at `http://localhost:9200` and Kibana at `http://localhost:5601`.
Security is disabled in this local development configuration; do not expose these ports publicly.

Run the simulation with indexing enabled:

```shell
./gradlew bootRun --args='--simulation.enabled=true --marketplace.elasticsearch.enabled=true'
```

The application creates explicit mappings and writes:

- `cars`: the 80 offers, with purchased cars updated to `SOLD`;
- `transactions`: completed purchases with buyer, seller, total cost, and completion time.

Every indexed simulation receives a unique `runId`. Car document IDs are also run-scoped, so runs
can execute without overwriting each other and previous results remain available for trend analysis.
Filter both Kibana data views by the same `runId` when presenting one simulation. The run ID is
printed at startup.

Import the included data views and five-visualization dashboard:

```shell
bash kibana/import-dashboard.sh
```

See [`kibana/README.md`](kibana/README.md) for dashboard contents and run filtering.

Car documents track `currentTransactionId`; a delayed return only changes availability
if that transaction still owns the car, so it cannot overwrite a later resale.
With Elasticsearch running locally, run the persistence regression test alongside the suite:

```shell
ELASTICSEARCH_INTEGRATION_TEST=true ./gradlew test --no-daemon
```

Stop the local services without deleting their data:

```shell
docker compose down
```
