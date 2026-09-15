package locator;

import org.openqa.selenium.By;

public class ResultsPageLocator {

    // Sol filtre panelindeki "Gidiş Kalkış / varış saatleri" bölümü
    public static final By TIME_FILTER_SECTION = By.cssSelector("[data-testid='filter-section-time']");
    public static final By TIME_FILTER_TOGGLE_BUTTON =
            By.cssSelector("[data-testid='filter-section-time'] [data-testid='filter-section-toggle-button']");

    // Bölüm içindeki İLK slider = Gidiş (Kalkış/departure) zaman aralığı.
    // (İkinci slider Varış/arrival zamanı için; Case 1 sadece kalkış saatini filtreliyor.)
    public static final By DEPARTURE_TIME_SLIDER_START =
            By.cssSelector("[data-testid='filter-section-time'] input[aria-label='search.timeSlider.startTime']");
    public static final By DEPARTURE_TIME_SLIDER_END =
            By.cssSelector("[data-testid='filter-section-time'] input[aria-label='search.timeSlider.endTime']");

    // Her uçuş kartındaki kalkış saati (data-testid içinde uçuş id'si değiştiği için '*=' ile eşleştiriyoruz)
    public static final By FLIGHT_DEPARTURE_TIMES =
            By.cssSelector("[data-testid*='flight-timeline-departure-departure-text-']");

    public static final By FLIGHT_ORIGIN_CODES =
            By.cssSelector("[data-testid*='flight-mini-route-departure-origin-']");
    public static final By FLIGHT_DESTINATION_CODES =
            By.cssSelector("[data-testid*='flight-mini-route-departure-destination-']");

    // ------------------------------------------------------------------
    // VARYANT TESPITI
    //
    // enuygun, "fly_search_facelift" adli bir A/B testi yurutuyor ve ayni
    // aramaya iki farkli sonuc sayfasi donebiliyor:
    //   B -> yeni tasarim (card/timeline)  : asagidaki locator'lar gecerli
    //   A -> klasik tasarim                : FlightListPageLocator gecerli
    //
    // Kova, oturum cerezine (weg_sid) bagli oldugu icin Selenium her koşuda
    // temiz profil actigindan rastgele degisiyor. Bu yuzden hangi tasarimin
    // geldigini calisma aninda tespit ediyoruz.
    //
    // DIKKAT: "div.flight-item" HER IKI tasarimda da bulundugu icin ayirt
    // edici DEGILDIR; asagidaki isaretler tasarima ozgudur.
    // ------------------------------------------------------------------
    public static final By FACELIFT_MARKER = By.cssSelector("[data-testid='filter-section-time']");
    public static final By CLASSIC_MARKER =
            By.cssSelector("[data-testid='resultSortingMainV2'], [data-testid='flightInfoPrice']");

    // ------------------------------------------------------------------
    // CASE 2 - Fiyat siralama ve havayolu filtresi (yeni tasarim)
    // ------------------------------------------------------------------

    /**
     * Ucus kartindaki fiyat elementi.
     * Onemli: gorunen metin "1.486 TL" seklinde yerellestirilmis; bunu parse
     * etmek yerine elementin data-price="1485.99" attribute'unu okuyoruz.
     * Boylece binlik ayraci / para birimi / yuvarlama kaynakli hatalar olmuyor.
     */
    public static final By FLIGHT_PRICES =
            By.cssSelector("[data-testid^='flight-price-departure-main-price-']");

    /** Havayolu adi, logo <img> etiketinin alt metninde bulunuyor (orn. alt="Pegasus"). */
    public static final By FLIGHT_AIRLINE_LOGOS =
            By.cssSelector("[data-testid^='flight-airline-logo-departure-single-image-container-'] img");

    /** Fiyata gore artan siralama ("En ucuz" butonu). */
    public static final By SORT_CHEAPEST_BUTTON =
            By.cssSelector("[data-testid='sorting-option-cheapest']");

    /** Sol paneldeki "Havayollari" filtre bolumu. */
    public static final By AIRLINES_FILTER_SECTION =
            By.cssSelector("[data-testid='filter-section-airlines']");
    public static final By AIRLINES_FILTER_TOGGLE =
            By.cssSelector("[data-testid='filter-section-airlines'] [data-testid='filter-section-title-button']");

    /** Bolumdeki tum havayolu checkbox'lari (data-testid'de IATA kodu var: ...-TK, ...-PC, ...-VF). */
    public static final By AIRLINE_CHECKBOXES =
            By.cssSelector("[data-testid^='filter-checkbox-input-airlines-']");

    /**
     * Havayolunu GORUNEN ADIYLA bulur (IATA kodu yerine).
     * Kod bazli locator (airlines-TK) kisa ama site kodu degistirirse sessizce
     * kirilir; gorunen ad testin niyetini de daha net anlatiyor.
     */
    public static By airlineCheckboxByName(String airlineName) {
        return By.xpath(
                "//*[@data-testid='filter-section-airlines']" +
                "//label[contains(normalize-space(.), \"" + airlineName + "\")]");
    }
}
