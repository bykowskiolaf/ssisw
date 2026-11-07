# Scenariusze testowe

Wyniki automatyczne uzyskano poleceniem `./gradlew test`. Wyniki integracyjne
uzyskano pełnym przebiegiem symulacji opisanym w `DEMO.md`.

| ID | Nazwa | Warunki początkowe | Agenci / komponenty | Przebieg | Oczekiwany rezultat | Uzyskany rezultat | Wynik |
|---|---|---|---|---|---|---|---|
| T01 | Wyszukanie pasującego auta | Dostępne auto zgodne z kryteriami | Buyer, Seller | Buyer wysyła `car-search` | Seller zwraca `PROPOSE` z najtańszą zgodną ofertą | Oferta została zwrócona i użyta w rankingu | PASS |
| T02 | Brak pasującego auta | Żadne dostępne auto nie spełnia filtrów | Buyer, Seller | Zapytanie z niemożliwymi kryteriami | Seller zwraca `REFUSE/NO_OFFER` | Brak oferty obsłużony bez przerwania symulacji | PASS |
| T03 | Zakup w budżecie | Budżet większy od uzgodnionego kosztu | Buyer, Seller | Rezerwacja, zakup, płatność | Auto `SOLD`, budżet pomniejszony o koszt | Transakcja utworzona, saldo poprawne | PASS |
| T04 | Zakup ponad budżet | Koszt całkowity większy od salda | BuyerAccount | Próba płatności | Operacja odrzucona, saldo bez zmian | `IllegalStateException`, saldo zachowane | PASS |
| T05 | Konkurencja o jedno auto | Kilku Buyer wybiera tę samą ofertę | Buyers, Seller | Równoległe żądania rezerwacji | Tylko jedno `AGREE`, pozostałe `REFUSE` | W logu odnotowano odrzucone konkurencyjne rezerwacje | PASS |
| T06 | Rezerwacja | Auto `AVAILABLE`, poprawna cena | Buyer, Seller | Buyer wysyła `car-reservation` | Status `RESERVED`, przypisany Buyer | Rezerwacja zaakceptowana | PASS |
| T07 | Wygaśnięcie rezerwacji | Rezerwacja bez zakupu | SellerInventory | Zegar przekracza 5 sekund | Auto automatycznie `AVAILABLE` | Test zegara potwierdził zwolnienie | PASS |
| T08 | Udana negocjacja | Oferta mieści się w limicie 15% | Buyer, Negotiator | Oferta i kontrpropozycje | Akceptacja w maks. 3 rundach | Cena zaakceptowana i użyta w transakcji | PASS |
| T09 | Odrzucona negocjacja | Limit kupującego poniżej ceny minimalnej | Buyer, Negotiator | Trzy rundy bez zgodnej ceny | Odrzucenie po limicie rund | Wynik `accepted=false`, liczba rund ograniczona | PASS |
| T10 | Rekomendacja TOP 3 | Co najmniej 4 zgodne oferty | Buyer, Advisor | Przekazanie ofert i preferencji | Trzy najwyższe wyniki MCDA | Zwrócono dokładnie TOP 3 w poprawnej kolejności | PASS |
| T11 | Ocena sprzedawcy | Potwierdzona transakcja | Buyer, Reputation | Oceny 5 i 4 dla sprzedawcy | Średnia 4,5; duplikat odrzucony | Średnia i licznik poprawne, duplikat odrzucony | PASS |
| T12 | Analiza trendu | Cztery sprzedaże o rosnących cenach | Buyer, Trend | Kolejne `market-sale-event` | Popularne kategorie i trend `RISING` | Agregaty oraz trend zgodne z danymi | PASS |
| T13 | Zwrot w terminie | Zakup młodszy niż 5 sekund | Buyer, Seller | `car-return` od właściciela | Refund, auto `AVAILABLE`, historia `RETURNED` | Pełny zwrot wykonany; Buyer kupił zamiennik | PASS |
| T14 | Zwrot po terminie | Od zakupu minęło ponad 5 sekund | Buyer, Seller | Żądanie zwrotu | `REFUSE`, auto nadal `SOLD` | Otrzymano `RETURN_WINDOW_EXPIRED` | PASS |
| T15 | Wartość rynkowa | Oferta i dwa porównywalne auta | Advisor / ValuationService | Obliczenie średniej i różnicy procentowej | Poprawny przedział 0–5, 5–15 lub >15% | Wszystkie granice przedziałów zweryfikowane | PASS |
| T16 | Wybór MCDA | Oferty różnią się ceną, rokiem, paliwem, pojemnością i spalaniem | Buyer, Advisor | Normalizacja i ważona suma | Ranking używa wag 30/20/15/10/10/15 | Kolejność i zakres wyniku 0–1 poprawne | PASS |
| T17 | Wielu agentów jednocześnie | 10 Seller, 5 Buyer, 80 aut | Wszystkie agenty | Pełna symulacja | 15 zachowanych zakupów bez podwójnej sprzedaży | 15 `SOLD`, 0 `RESERVED`, każdy Buyer ma 3 auta | PASS |

## Podsumowanie

- Scenariusze: **17**
- Zaliczone: **17**
- Niezaliczone: **0**
- Pokryte obszary kryteriów oceny: komunikacja, budżet, konkurencja, rezerwacja,
  rekomendacje, negocjacje, reputacja, trendy, zwroty, wycena i MCDA.
