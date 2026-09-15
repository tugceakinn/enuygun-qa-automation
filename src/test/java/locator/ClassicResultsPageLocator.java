package locator;

import org.openqa.selenium.By;

/**
 * enuygun'un KLASİK sonuç sayfası tasarımına ait locator'lar
 * (A/B testi "fly_search_facelift" = A).
 *
 * Yeni tasarım için locator'lar {@link ResultsPageLocator} içinde.
 * Hangi tasarımın geldiği {@link page.ResultsVariant} ile tespit edilir.
 */
public class ClassicResultsPageLocator {

    /** Uçuş kartları. */
    public static final By FLIGHT_ITEMS = By.cssSelector("div.flight-item[data-flight-id]");

    /** Kart üzerindeki kalkış saati (örn. "07:45"). */
    public static final By FLIGHT_DEPARTURE_TIMES =
            By.cssSelector("div.flight-item[data-flight-id] [data-testid='departureTime']");

    /**
     * Fiyat elementi. Görünen metin "1.486 TL" şeklinde yerelleştirilmiş;
     * bunun yerine data-price="1485.99" attribute'unu okuyoruz.
     * (Yeni tasarımda da aynı yaklaşım kullanılıyor.)
     */
    public static final By FLIGHT_PRICES =
            By.cssSelector("div.flight-item[data-flight-id] [data-testid='flightInfoPrice']");

    /** Havayolu adı (örn. "Pegasus"). */
    public static final By FLIGHT_AIRLINES =
            By.cssSelector("div.flight-item[data-flight-id] .summary-marketing-airlines");

    /**
     * Rota bilgisi. İçindeki .itemAirport span'lerinin İLKİ kalkış,
     * SONUNCUSU varış havalimanıdır (aktarmalı uçuşlarda arada duraklar olur).
     */
    public static final By ROUTE_CONTAINERS =
            By.cssSelector("div.flight-item[data-flight-id] [data-testid='airportSummarySteps']");
    public static final By ROUTE_AIRPORT_ITEMS = By.cssSelector(".itemAirport");

    /** Fiyata göre artan sıralama butonu. */
    public static final By SORT_PRICE_ASC = By.cssSelector(".sort-buttons.search__filter_sort-PRICE_ASC");

    /** "Gidiş kalkış / varış saatleri" filtre akordeonu başlığı. */
    public static final By TIME_FILTER_HEADER = By.cssSelector(".ctx-filter-departure-return-time");

    /**
     * Saat filtresi slider'ları. Bu tasarımda native <input type="range"> YOK;
     * rc-slider kullanılıyor ve handle'lar role="slider" olan <div>'ler.
     * Değerler DAKİKA cinsinden (aria-valuemin=0, aria-valuemax=1439).
     * İlk .rc-slider = gidiş kalkış saati, ikincisi = varış saati.
     */
    public static final By TIME_SLIDERS = By.cssSelector(".rc-slider");
    public static final By SLIDER_HANDLES = By.cssSelector("[role='slider']");

    /** Havayolu filtresi akordeonu. */
    public static final By AIRLINE_FILTER_HEADER = By.cssSelector(".ctx-filter-airline");

    public static By airlineFilterLabelByName(String airlineName) {
        return By.xpath("//label[contains(normalize-space(.), \"" + airlineName + "\")]");
    }
}
