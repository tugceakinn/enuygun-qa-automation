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

    // ---- Part 4: veri kazima icin ek alanlar ----
    public static final By FLIGHT_ARRIVAL_TIMES =
            By.cssSelector("div.flight-item[data-flight-id] [data-testid='arrivalTime']");
    public static final By FLIGHT_DURATIONS =
            By.cssSelector("div.flight-item[data-flight-id] [data-testid='departureFlightTime']");
    /** "Direkt Uçuş" / "1 aktarma" bilgisi. */
    public static final By FLIGHT_TRANSIT = By.cssSelector(".summary-transit");

    /** Kart ici okumalar icin (kart bazli iterasyon). */
    public static final By CARD_AIRLINE = By.cssSelector(".summary-marketing-airlines");
    public static final By CARD_DEPARTURE_TIME = By.cssSelector("[data-testid='departureTime']");
    public static final By CARD_ARRIVAL_TIME = By.cssSelector("[data-testid='arrivalTime']");
    public static final By CARD_DURATION = By.cssSelector("[data-testid='departureFlightTime']");
    public static final By CARD_PRICE = By.cssSelector("[data-testid='flightInfoPrice']");

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

    // ------------------------------------------------------------------
    // CASE 3 - Uçuş seçimi / rezervasyona ilerleme akışı
    //
    // Gidiş-dönüş aramada akış 4 adım (canlı sitede doğrulandı):
    //   1) Gidiş listesinde "Seç"          -> paket seçenekleri açılır
    //   2) "Seç ve İlerle"                 -> dönüş listesi görünür hale gelir
    //   3) Dönüş listesinde "Seç"          -> paket seçenekleri açılır
    //   4) Dönüş paketine tıklama          -> DOĞRUDAN rezervasyon sayfasına gider
    //      (bu adımda ayrı bir "ilerle" butonu YOK)
    // ------------------------------------------------------------------

    public static final By DEPARTURE_FLIGHT_LIST = By.cssSelector(".flight-list-departure");
    public static final By RETURN_FLIGHT_LIST = By.cssSelector(".flight-list-return");

    /** Kart üzerindeki "Seç" butonu. */
    public static final By SELECT_FLIGHT_BUTTON = By.cssSelector("button.action-select-btn");

    /** "Seç" sonrası açılan paket alanı. */
    public static final By OPENED_PACKAGE_WRAPPER = By.cssSelector(".flight-item__wrapper.package-opened");

    /** Gidiş paketinden sonraki "Seç ve İlerle" butonu. */
    public static final By PROVIDER_SELECT_BUTTON = By.cssSelector("button[data-testid='providerSelectBtn']");

    /** Dönüş paket kartı - tıklanınca rezervasyon sayfasına yönlendirir. */
    public static final By RETURN_PACKAGE_ITEM =
            By.cssSelector("[data-testid^='returnProviderPackageItem']");

    public static By airlineFilterLabelByName(String airlineName) {
        return By.xpath("//label[contains(normalize-space(.), \"" + airlineName + "\")]");
    }
}
