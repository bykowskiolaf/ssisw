# Sprawozdanie LaTeX

Główny dokument: `main.tex`. Korzysta z dostarczonej klasy `uzreport.cls`
i logo `assets/logo.pdf` (przekonwertowane z `assets/logo.eps`).
Oryginalny raport z innego przedmiotu zachowano
w `template-original.tex`.

## Kompilacja

Z katalogu `docs/report/`:

```sh
latexmk -pdf -interaction=nonstopmode -halt-on-error -outdir=build main.tex
```

Wynik: `build/main.pdf`. Wymagany jest zestaw LaTeX z pakietami używanymi
przez klasę (np. pełny TeX Live / MacTeX) oraz `latexmk`.
Dołączone logo PDF nie wymaga konwersji EPS podczas kompilacji.

Jeżeli kompilacja zgłasza `No hyphenation patterns for language 'polish'`,
włącz lub zainstaluj polskie wzorce dzielenia wyrazów w swojej dystrybucji
TeX i przebuduj konfigurację oraz formaty. W lokalnym MiKTeX 22.1 użyto:

```sh
miktex languages configure
miktex formats build --key=pdflatex
```

Pełna kompilacja PDF została zweryfikowana po naprawie konfiguracji MiKTeX.

## Przed oddaniem

- Autor: Olaf Bykowski; grupa: `12INF-ISI-SD(L)`.
- Prowadzący: Dr inż. Marek Wróblewski.
- Dołącz własne zrzuty Kibany, jeśli wymagany jest dowód uruchomienia wizualizacji.
- Wyniki 15 zakupów i przebiegu ze zwrotem pochodzą z dokumentacji projektu.
  Szczegółowe wyniki negocjacji, reputacji i trendów pochodzą z przypadków
  testowych. Liczbowe oceny MCDA w tabeli są obliczeniami na danych testowych,
  nie pomiarami pełnej symulacji.

Pliki treści: `implementation.tex`, `tests.tex`, `results.tex`.
