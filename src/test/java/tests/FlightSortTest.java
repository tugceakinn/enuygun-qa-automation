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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FlightSortTest extends BaseTest {

    /**
     * Case 2: Price Sorting for Turkish Airlines
     *
     * - İstanbul -> Ankara araması yapılır (şehirler ve tarihler parametrik)
     * - Sonuçlar sadece Türk Hava Yolları olacak şekilde filtrelenir
     * - Fiyata göre artan sıralama uygulanır
     * - Doğrulanır: liste dolu, tüm uçuşlar THY, fiyatlar artan sırada
     */
    @Test
    @Parameters({"originCity", "destinationCity", "departureDate", "returnDate", "airlineName"})
    public void testPriceSortingForTurkishAirlines(
            @Optional("İstanbul") String originCity,
            @Optional("Ankara") String destinationCity,
            @Optional("2026-09-20") String departureDate,
            @Optional("2026-09-25") String returnDate,
            @Optional("Türk Hava Yolları") String airlineName) {

        HomePage homePage = new HomePage(getDriver());

        homePage.goToEnuygun()
                .acceptCookies()
                .selectRoundTrip()
                .selectOrigin(originCity)
                .selectDestination(destinationCity)
                .selectDates(departureDate, returnDate)
                .clickSearchButton();

        // Site bir A/B testi yürüttüğü için sonuç sayfası iki farklı tasarımda
        // gelebiliyor; factory hangisinin geldiğini tespit edip uygun
        // implementasyonu döner. (Detaylı açıklama: ResultsVariant sınıfı)
        FlightResults results = FlightResultsFactory.create(getDriver(), Duration.ofSeconds(40));
        results.filterByAirlineOnly(airlineName)
               .sortByPriceAscending();

        // 1) Filtreleme sonrası liste boş kalmamalı
        List<Double> prices = results.getDisplayedPrices();
        Assert.assertFalse(prices.isEmpty(),
                "'" + airlineName + "' filtresi sonrası hiç uçuş listelenmedi.");

        // 2) Listelenen tüm uçuşlar seçilen havayoluna ait olmalı
        List<String> airlines = results.getDisplayedAirlines();
        Set<String> distinctAirlines = airlines.stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        Assert.assertEquals(distinctAirlines, Set.of(airlineName),
                "Filtre sonrası beklenen tek havayolu '" + airlineName +
                "' iken listede şunlar var: " + distinctAirlines);

        // 3) Fiyatlar artan sırada olmalı
        List<Double> sorted = prices.stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(prices, sorted,
                "Fiyatlar artan sırada değil. Ekrandaki sıra: " + prices +
                " / beklenen sıra: " + sorted);
    }
}
