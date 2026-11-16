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
./gradlew bootRun --no-daemon --args='--simulation.enabled=true --simulation.demo-mode=true --simulation.demonstrate-return=true --simulation.jade-gui=true --simulation.timeout-seconds=120 --marketplace.elasticsearch.enabled=true'
```

Launcher wyłącza domyślny tryb headless Spring Boot przed uruchomieniem JADE,
aby umożliwić wyświetlenie okna Swing agenta `rma`. Polecenie należy wykonać
w lokalnej sesji graficznej, nie przez SSH bez dostępu do ekranu.
Nie ustawiaj `-Djava.awt.headless=true` w opcjach JVM.

Tryb `--simulation.demo-mode=true` daje **15 sekund przed pierwszym zapytaniem
zakupowym** na uruchomienie Sniffera i dodanie obserwowanych agentów. Następnie
każdy kupujący czeka **1 sekundę między kolejnymi cyklami wyszukiwania**, także
po konflikcie rezerwacji. W Snifferze dodaj kupujących, sprzedających i agentów
usługowych do obserwacji w trakcie początkowej pauzy.

Opcjonalne ustawienia:

- `--simulation.demo-startup-delay-ms=30000`: 30 sekund na przygotowanie Sniffera;
- `--simulation.demo-cycle-delay-ms=2000`: 2 sekundy między cyklami kupującego.

Pauzy nie blokują wątku agenta i nie opóźniają zakupu po rezerwacji ani zwrotu.
Okna rezerwacji i zwrotu nadal wynoszą 5 sekund. Początkowa pauza jest dodawana
do limitu czasu symulacji; przy dużych opóźnieniach cykli zwiększ również
`simulation.timeout-seconds`. Bez `simulation.demo-mode=true` obie pauzy są
wyłączone.

**Uwaga:** `simulation.timeout-seconds=120` to maksymalny czas oczekiwania na
zakończenie symulacji, a nie czas prezentacji GUI. Po wypisaniu podsumowania
udany przebieg GUI pozostawia platformę otwartą przez dodatkowe **60 sekund**.
Dodaj `--simulation.gui-keep-open-seconds=300`, aby wydłużyć ten czas do pięciu
minut, albo `=0`, aby wyłączyć oczekiwanie. Ctrl+C kończy aplikację wcześniej.
Tryb bez GUI nie czeka. Kupujący usuwają się z platformy po wykonaniu swoich
zakupów, więc dodatkowy czas pozwala oglądać pozostałych agentów i okno GUI,
ale nie zachowuje pełnej początkowej listy kupujących.

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
