# ChronoDeck (prototyp 0.1.1)

Natywna aplikacja Android do lokalnego harmonogramu wydarzeń w blokach czasu. Nie korzysta z Google Calendar, nie ma reklam, konta, telemetrii ani uprawnienia INTERNET.

## Funkcje w tej wersji

- własne wydarzenia na dziś z godziną rozpoczęcia i zakończenia;
- lokalna baza SQLite (`chronodeck.db`);
- oś czasu dnia z linią „TERAZ”;
- statusy: PLAN, W TOKU, ZAKOŃCZONE, WYKONANE;
- mały widżet: zegar, wybrane wydarzenie, następne wydarzenie, pasek postępu oraz przyciski START / STOP / DONE;
- odświeżanie widżetu przez nieprecyzyjny `AlarmManager` oraz po zmianie czasu/daty;
- odświeżanie zegara i widżetu co minutę, gdy główna aplikacja jest otwarta;
- kontrola dozwolonych statusów w lokalnej bazie;
- walidacja formularza nie zamyka okna, gdy nazwa lub godziny są niepoprawne.

## Kompilacja w GitHub Actions

Repozytorium zawiera workflow `.github/workflows/build-apk.yml`. Po wypchnięciu projektu do repozytorium GitHub kompilacja uruchamia się automatycznie. Po zakończeniu zadania otwórz kartę **Actions**, wybierz uruchomienie i pobierz artefakt `ChronoDeck-debug-apk`. W archiwum znajdziesz `app-debug.apk`. To APK debug, nie podpisane wydanie produkcyjne.

Workflow instaluje JDK 17, Android SDK Platform 35 i Build Tools 35.0.0, generuje Gradle Wrapper w środowisku CI, a następnie uruchamia `assembleDebug`. APK jest publikowany jako artefakt na 14 dni.

## Ograniczenia i status testów

- W tej wersji widok wydarzeń obejmuje tylko bieżący dzień; nie ma jeszcze nawigacji do innych dni, edycji istniejącego wydarzenia ani powtarzania.
- Widżety Androida nie gwarantują sekundowego ani dokładnie minutowego odświeżania. Android może opóźniać odświeżenie, zwłaszcza w trybie oszczędzania energii.
- Kompilacja APK musi zostać potwierdzona przez GitHub Actions; nie należy uznawać jej za udaną, dopóki workflow nie zakończy się poprawnie.
