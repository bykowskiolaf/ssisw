# Scenariusz prezentacji

## Przygotowanie

Gradle należy uruchamiać na JDK 25; kompilacja korzysta z toolchain Java 27.

```shell
export JAVA_HOME=/Users/bykowski/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home
./gradlew test --no-daemon
```

Jeżeli `data/cars.json` nie istnieje:

```shell
./gradlew bootRun --no-daemon --args='--cars.import.enabled=true'
```

## Prezentacja pełnej symulacji

1. Uruchom Elasticsearch i Kibana: `docker compose up -d`.
2. Zaimportuj dashboard: `bash kibana/import-dashboard.sh`.
3. Uruchom JADE z GUI, zwrotem i indeksowaniem:

```shell
./gradlew bootRun --no-daemon --args='--simulation.enabled=true --simulation.demonstrate-return=true --simulation.jade-gui=true --simulation.timeout-seconds=120 --marketplace.elasticsearch.enabled=true'
```

4. W JADE GUI pokaż 10 `Seller`, 5 `Buyer`, `Advisor`, `Negotiator`,
   `Reputation` i `Trend`.
5. W logu pokaż kolejno: oferty, TOP 3, `VALUATION`, `NEGOTIATION`, odrzuconą
   konkurencyjną rezerwację, `PURCHASE`, `RETURN`, `RATING` i `TREND`.
6. Zanotuj wypisany `runId`.
7. Otwórz dashboard **SSISW — giełda samochodowa** w Kibana i dodaj filtr
   `runId: <wartość_z_logu>`.
8. Pokaż podsumowanie: 16 rekordów zakupów, 1 zwrot, 15 zachowanych zakupów,
   65 `AVAILABLE`, 0 `RESERVED`, 15 `SOLD`.

Bez ELK pełną logikę można zaprezentować poleceniem:

```shell
./gradlew bootRun --no-daemon --args='--simulation.enabled=true --simulation.demonstrate-return=true'
```

## Zatrzymanie

```shell
docker compose down
```

Polecenie nie usuwa wolumenu ani danych poprzednich przebiegów.
