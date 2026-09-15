package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import page.FlightResults;
import page.FlightResultsFactory;
import page.HomePage;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FlightSearchTest extends BaseTest {

    /**
     * Case 1: Basic Flight Search and Time Filter
     *
     * - İstanbul -> Ankara gidiş-dönüş arama (şehirler ve tarihler parametrik)
     * - Sonuç listesinde kalkış saati filtresi (10:00 - 18:00) uygulanır
     * - Doğrulanır: liste dolu, tüm kalkış saatleri aralıkta, rota eşleşiyor
     */
    @Test
    @Parameters({"originCity", "destinationCity", "departureDate", "returnDate",
                 "filterStartHour", "filterEndHour",
                 "originAirportCodes", "destinationAirportCodes"})
    public void testBasicFlightSearchWithTimeFilter(
            @Optional("İstanbul") String originCity,
            @Optional("Ankara") String destinationCity,
            @Optional("2026-09-20") String departureDate,
            @Optional("2026-09-25") String returnDate,
            @Optional("10") int filterStartHour,
            @Optional("18") int filterEndHour,
            @Optional("IST,SAW") String originAirportCodes,
            @Optional("ESB") String destinationAirportCodes) {

        HomePage homePage = new HomePage(getDriver());

        homePage.goToEnuygun()
                .acceptCookies()
                .selectRoundTrip()
                .selectOrigin(originCity)
                .selectDestination(destinationCity)
                .selectDates(departureDate, returnDate)
                .clickSearchButton();

        // Site bir A/B testi yürütüyor ("fly_search_facelift"): aynı aramaya iki
        // farklı sonuç sayfası dönebiliyor ve kova oturum çerezine bağlı olduğu
        // için Selenium'un her koşuda açtığı temiz profilde değişiyor.
        // Factory hangi tasarımın geldiğini tespit edip uygun implementasyonu verir.
        FlightResults results = FlightResultsFactory.create(getDriver(), Duration.ofSeconds(40));
        results.applyDepartureTimeFilter(filterStartHour, filterEndHour);

        // 1) Uçuş listesi düzgün gösteriliyor mu?
        Assert.assertTrue(results.getDisplayedFlightCount() > 0,
                "Filtre sonrası hiç uçuş listelenmedi, sonuç listesi düzgün gösterilmiyor.");

        // 2) Tüm gösterilen uçuşların kalkış saati seçilen aralıkta mı?
        List<LocalTime> departureTimes = results.getDisplayedDepartureTimes();
        Assert.assertFalse(departureTimes.isEmpty(),
                "Uçuş kartlarından hiç kalkış saati okunamadı.");

        List<LocalTime> outOfRange = departureTimes.stream()
                .filter(t -> t.getHour() < filterStartHour || t.getHour() > filterEndHour)
                .collect(Collectors.toList());

        Assert.assertTrue(outOfRange.isEmpty(),
                "Filtre aralığı (" + filterStartHour + ":00 - " + filterEndHour + ":00) dışında " +
                "kalkış saati olan uçuşlar var: " + outOfRange);

        // 3) Sonuçlar seçilen rota ile eşleşiyor mu?
        //
        //    DİKKAT: "tüm kartlarda TEK bir havalimanı kodu olmalı" varsayımı
        //    yanlıştır - İstanbul'un iki havalimanı var (IST ve SAW) ve şehir
        //    bazlı aramada ikisi birden listelenir. Doğru kontrol, görünen tüm
        //    kodların o şehre ait havalimanları arasında olmasıdır.
        Set<String> expectedOrigins = toCodeSet(originAirportCodes);
        Set<String> expectedDestinations = toCodeSet(destinationAirportCodes);

        Set<String> actualOrigins = Set.copyOf(results.getDisplayedOriginCodes());
        Set<String> actualDestinations = Set.copyOf(results.getDisplayedDestinationCodes());

        Assert.assertFalse(actualOrigins.isEmpty(), "Sonuçlarda hiç kalkış havalimanı kodu okunamadı.");
        Assert.assertTrue(expectedOrigins.containsAll(actualOrigins),
                "Sonuçlarda '" + originCity + "' şehrine ait olmayan kalkış havalimanı var. " +
                "Beklenen: " + expectedOrigins + ", bulunan: " + actualOrigins);

        Assert.assertFalse(actualDestinations.isEmpty(), "Sonuçlarda hiç varış havalimanı kodu okunamadı.");
        Assert.assertTrue(expectedDestinations.containsAll(actualDestinations),
                "Sonuçlarda '" + destinationCity + "' şehrine ait olmayan varış havalimanı var. " +
                "Beklenen: " + expectedDestinations + ", bulunan: " + actualDestinations);
    }

    private static Set<String> toCodeSet(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
