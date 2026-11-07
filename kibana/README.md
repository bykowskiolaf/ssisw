# Kibana dashboard

`dashboard.ndjson` zawiera dwa data views, pięć wizualizacji Vega oraz dashboard
**SSISW — giełda samochodowa**:

1. liczba samochodów według marki,
2. liczba samochodów według typu nadwozia,
3. liczba samochodów według paliwa,
4. rozkład roku produkcji,
5. liczba transakcji według statusu `COMPLETED` / `RETURNED`.

Po uruchomieniu Compose i wykonaniu przynajmniej jednego indeksowanego przebiegu:

```shell
bash kibana/import-dashboard.sh
```

Adres Kibana można zmienić zmienną `KIBANA_URL`. Import używa `overwrite=true`,
więc polecenie jest powtarzalne.

Każdy przebieg ma unikalny `runId`. Na dashboardzie ustaw filtr:

```text
runId: "identyfikator wypisany przez aplikację"
```

Wizualizacje Vega odwołują się bezpośrednio do indeksów `cars` i
`transactions`, a filtr dashboardu jest przekazywany przez `%context%`.
