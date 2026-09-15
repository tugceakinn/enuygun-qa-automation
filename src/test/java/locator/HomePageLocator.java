package locator;

import org.openqa.selenium.By;

public class HomePageLocator {

    // Gidiş-dönüş radio butonu: gerçek <input type=radio> CSS ile görsel olarak
    // gizlenmiş (visibility:hidden, 0x0), bu yüzden Selenium onu tıklanabilir bulmuyor.
    // Tıklanabilir olan, onu saran <label data-testid='search-round-trip-label'> elemanı.
    public static final By ROUND_TRIP_RADIO = By.cssSelector("[data-testid='search-round-trip-label']");

    // Nereden / Nereye kutuları
    public static final By ORIGIN_CONTAINER = By.cssSelector("[data-testid='endesign-flight-origin-autosuggestion']");
    public static final By ORIGIN_INPUT = By.cssSelector("[data-testid='endesign-flight-origin-autosuggestion-input']");

    public static final By DESTINATION_CONTAINER = By.cssSelector("[data-testid='endesign-flight-destination-autosuggestion']");
    public static final By DESTINATION_INPUT = By.cssSelector("[data-testid='endesign-flight-destination-autosuggestion-input']");

    // Şehir kutusunu açtıktan sonra listede (popüler ya da filtrelenmiş) görünen
    // şehir adını içeren <li> öğesini bulan generic locator. Container WebElement
    // üzerinden findElement(...) ile relative olarak kullanılmalı.
    public static By suggestionByCityName(String cityName) {
        return By.xpath(".//li[contains(normalize-space(.), '" + cityName + "')]");
    }

    // NOT: Site ekran genişliğine göre FARKLI DOM yapıları kullanıyor.
    // Masaüstü genişlikte (Selenium'un maximize() ile açtığı pencere budur) aşağıdaki
    // '-datepicker-input' son ekli testid'ler gerçek <input> elemanlarıdır ve doğru
    // olanlar bunlardır. (Dar/mobil genişlikte bu son ek olmadan farklı bir container
    // div kullanılıyor — test masaüstü Chrome ile koştuğu için bunlar geçerli değil.)
    public static final By DEPARTURE_DATE = By.cssSelector("[data-testid='enuygun-homepage-flight-departureDate-datepicker-input']");
    public static final By RETURN_DATE = By.cssSelector("[data-testid='enuygun-homepage-flight-returnDate-datepicker-input']");

    // Takvimde gün hücreleri <button title='YYYY-MM-DD' data-testid='datepicker-active-day'>
    public static By dynamicDate(String date) {
        return By.cssSelector("[data-testid='datepicker-active-day'][title='" + date + "']");
    }

    // Takvim açıldığında hedef tarih görünen ayda olmayabilir. Takvimde sadece
    // "ileri ay" butonu var (geri butonu yok, geçmişe gidilemiyor), bu yüzden hedef
    // tarih bulunana kadar bu butona basmamız gerekebilir.
    // fieldName: "departureDate" ya da "returnDate"
    public static By monthForwardButton(String fieldName) {
        return By.cssSelector("[data-testid='enuygun-homepage-flight-" + fieldName + "-month-forward-button']");
    }

    // Gidiş ve dönüş takvim popup'larının kapsayıcı container'ı. Gün / ileri-ay
    // aramalarını buna göre SINIRLANDIRIYORUZ: iki takvim paneli (gidiş/dönüş)
    // bazı durumlarda aynı anda DOM'da bulunabiliyor (biri kapalı/görünmez halde);
    // scope'suz bir arama yanlış panelden eşleşme riski taşır.
    // fieldName: "departureDate" ya da "returnDate"
    public static By datepickerContainer(String fieldName) {
        return By.cssSelector("[data-testid='enuygun-homepage-flight-" + fieldName + "-datepicker']");
    }

    public static final By ACTIVE_DAY_ANY = By.cssSelector("[data-testid='datepicker-active-day']");

    /**
     * Seçilebilir VE seçilemez (rezervasyon aralığı dışı) tüm gün hücreleri.
     * "Takvim gerçekten ilerliyor mu?" kontrolünde kullanılıyor - aralığın
     * sonuna gelindiğinde günler passive olduğu için sadece active'e bakmak
     * yanıltıcı olurdu.
     */
    public static final By ANY_DAY = By.cssSelector(
            "[data-testid='datepicker-active-day'], [data-testid='datepicker-passive-day']");

    // NOT: masaüstünde gerçek testid enuygun-homepage-flight-submitButton'tır
    // (dar/mobil genişlikte enuygun-homepage-flight-search-button kullanılıyor).
    public static final By SEARCH_BUTTON = By.cssSelector("[data-testid='enuygun-homepage-flight-submitButton']");

    public static final By ACCEPT_COOKIES = By.xpath("//button[contains(text(), 'KABUL ET') or contains(text(), 'Kabul Et')]");
}
