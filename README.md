# SSISW car market

Multi-agent car-market project built with Java, Spring Boot, JADE, Elasticsearch, and Kibana.

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

The first milestone starts 10 seller agents, 5 buyer agents, and allocates 8 cars to every seller.
Buyers discover sellers through the JADE Directory Facilitator, request offers with ACL messages,
compare total costs, reserve one available car, and complete one purchase. Broad shared preferences
intentionally create competition; buyers whose reservation is rejected search again. The process
prints a transaction summary and shuts down automatically.

Enable JADE's management GUI when inspecting agents and messages manually:

```shell
./gradlew bootRun --args='--simulation.enabled=true --simulation.jade-gui=true --simulation.timeout-seconds=120'
```

The simulation currently uses `data/cars.json`. If it is absent, run the importer first. Useful
settings are available under `simulation.*` in `application.properties`.
