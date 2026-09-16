package page;

import base.BasePage;
import locator.ClassicResultsPageLocator;
import model.FlightRecord;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * enuygun'un KLASİK sonuç sayfası tasarımı (A/B testi "fly_search_facelift" = A).
 *
 * Yeni tasarımdan en önemli farkı saat filtresi: burada native
 * <input type="range"> YOK. Bunun yerine rc-slider kullanılıyor; handle'lar
 * role="slider" olan <div>'ler ve değerler DAKİKA cinsinden (0-1439).
 * Bu yüzden yeni tasarımda işe yarayan "native setter + input event" yöntemi
 * burada uygulanamaz; handle'ları piksel hesabıyla sürüklemek gerekiyor.
 */
public class ClassicResultsPage extends BasePage implements FlightResults {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MINUTES_MIN = 0;
    private static final int MINUTES_MAX = 1439;

    private final WebDriverWait resultsWait;

    public ClassicResultsPage(WebDriver driver) {
        super(driver);
        this.resultsWait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    // ==================================================================
    // CASE 1 - Kalkış saati filtresi
    // ==================================================================

    @Override
    public ClassicResultsPage applyDepartureTimeFilter(int fromHour, int toHour) {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.FLIGHT_DEPARTURE_TIMES));

        openAccordion(ClassicResultsPageLocator.TIME_FILTER_HEADER);

        List<WebElement> sliders = resultsWait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(ClassicResultsPageLocator.TIME_SLIDERS));

        if (sliders.isEmpty()) {
            throw new IllegalStateException(
                    "Klasik tasarımda saat filtresi slider'ı bulunamadı. " +
                    "Filtre akordeonu açılmamış olabilir.");
        }

        // İlk slider = gidiş kalkış saati (ikincisi varış saati için).
        WebElement departureSlider = sliders.get(0);
        List<WebElement> handles = departureSlider.findElements(ClassicResultsPageLocator.SLIDER_HANDLES);

        if (handles.size() < 2) {
            throw new IllegalStateException(
                    "Kalkış saati slider'ında 2 handle bekleniyordu, " + handles.size() + " bulundu.");
        }

        dragHandleToMinutes(departureSlider, handles.get(0), fromHour * 60);
        dragHandleToMinutes(departureSlider, handles.get(1), toHour * 60);

        waitForResultsToReflectFilter(fromHour, toHour);
        return this;
    }

    private int readHandleMinutes(WebElement handle) {
        String raw = handle.getDomAttribute("aria-valuenow");
        int value = Integer.parseInt(raw.trim());
        // Bitiş handle'ı bazen max'ın 1 üstünü (1440) raporluyor; sınıra çekiyoruz.
        return Math.max(MINUTES_MIN, Math.min(MINUTES_MAX, value));
    }

    /**
     * rc-slider handle'ını hedef dakikaya sürükler.
     *
     * BUG (bulundu, canlı sitede doğrulandı): rc-slider, tek adımlı bir
     * sürüklemeyi (clickAndHold -> TEK moveByOffset -> release, ör. Selenium'un
     * dragAndDropBy'ı tam olarak bunu yapar) YOK SAYIYOR - handle hiç hareket
     * etmiyor. rc-slider'ın kendi mantığı, gerçek bir kullanıcı sürüklemesindeki
     * gibi ARDIŞIK mousemove olayları bekliyor. Bu yüzden hedefe TEK sıçrama
     * yerine birden fazla küçük ara adımla ilerliyoruz.
     */
    private void dragHandleToMinutes(WebElement slider, WebElement handle, int targetMinutes) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int current = readHandleMinutes(handle);
            if (current == targetMinutes) {
                return;
            }

            int trackWidth = slider.getSize().getWidth();
            if (trackWidth <= 0) {
                throw new IllegalStateException("Slider genişliği 0 okundu; element görünür değil olabilir.");
            }

            double pxPerMinute = (double) trackWidth / (MINUTES_MAX - MINUTES_MIN);
            int deltaX = (int) Math.round((targetMinutes - current) * pxPerMinute);

            if (deltaX == 0) {
                return;
            }

            scrollToElement(handle);
            dragByWithIntermediateSteps(handle, deltaX);
        }

        int finalValue = readHandleMinutes(handle);
        // rc-slider adım büyüklüğü nedeniyle birkaç dakikalık sapma normal.
        if (Math.abs(finalValue - targetMinutes) > 15) {
            throw new IllegalStateException(
                    "Saat slider'ı hedefe ulaşmadı: istenen=" + targetMinutes +
                    " dk, gerçek=" + finalValue + " dk. Sürükleme piksel hesabı hatalı olabilir.");
        }
    }

    /**
     * Bir elementi mevcut konumundan yatayda deltaX kadar, TEK sıçrama yerine
     * birden fazla küçük ara adımla sürükler. rc-slider gibi sürükleme
     * kütüphaneleri, gerçek kullanıcı hareketini taklit eden ardışık mousemove
     * olayları olmadan değeri güncellemiyor (bkz. dragHandleToMinutes yorumu).
     */
    private void dragByWithIntermediateSteps(WebElement element, int deltaX) {
        Actions actions = new Actions(driver);
        actions.moveToElement(element).clickAndHold();

        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            int stepDelta = (deltaX * i / steps) - (deltaX * (i - 1) / steps);
            actions.moveByOffset(stepDelta, 0);
        }

        actions.release().perform();
    }

    /** Filtre uygulandıktan sonra listenin güncellenmesini bekler (doğrulamayı test yapar). */
    private void waitForResultsToReflectFilter(int fromHour, int toHour) {
        PollingSupport.waitQuietly(resultsWait, d -> {
            List<LocalTime> times = getDisplayedDepartureTimes();
            if (times.isEmpty()) {
                return false;
            }
            return times.stream().allMatch(t -> t.getHour() >= fromHour && t.getHour() <= toHour);
        });
    }

    // ==================================================================
    // CASE 2 - Havayolu filtresi + fiyat sıralaması
    // ==================================================================

    @Override
    public ClassicResultsPage filterByAirlineOnly(String airlineName) {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.FLIGHT_ITEMS));

        openAccordion(ClassicResultsPageLocator.AIRLINE_FILTER_HEADER);

        WebElement label = resultsWait.until(ExpectedConditions.presenceOfElementLocated(
                ClassicResultsPageLocator.airlineFilterLabelByName(airlineName)));

        // Bu tasarımda checkbox'lar varsayılan olarak İŞARETSİZ gelir ve
        // "işaretsiz" = "filtre yok" anlamına gelir. Dolayısıyla sadece hedef
        // havayolunu işaretlemek "yalnızca o havayolu" sonucunu verir.
        List<WebElement> inputs = label.findElements(org.openqa.selenium.By.cssSelector("input[type='checkbox']"));
        boolean alreadySelected = !inputs.isEmpty() && inputs.get(0).isSelected();

        if (!alreadySelected) {
            clickSafely(label);
        }

        waitForAirlineFilterToApply(airlineName);
        return this;
    }

    private void waitForAirlineFilterToApply(String airlineName) {
        PollingSupport.waitQuietly(resultsWait, d -> {
            List<String> airlines = getDisplayedAirlines();
            return !airlines.isEmpty()
                    && airlines.stream().allMatch(a -> a.equalsIgnoreCase(airlineName));
        });
    }

    @Override
    public ClassicResultsPage sortByPriceAscending() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.FLIGHT_ITEMS));

        WebElement sortButton = resultsWait.until(ExpectedConditions.elementToBeClickable(
                ClassicResultsPageLocator.SORT_PRICE_ASC));
        clickSafely(sortButton);

        PollingSupport.waitQuietly(resultsWait, d -> {
            List<Double> prices = getDisplayedPrices();
            return !prices.isEmpty() && isAscending(prices);
        });
        return this;
    }

    // ==================================================================
    // Okuma metotları
    // ==================================================================

    @Override
    public List<LocalTime> getDisplayedDepartureTimes() {
        return driver.findElements(ClassicResultsPageLocator.FLIGHT_DEPARTURE_TIMES).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> LocalTime.parse(t, TIME_FORMAT))
                .collect(Collectors.toList());
    }

    @Override
    public List<Double> getDisplayedPrices() {
        return driver.findElements(ClassicResultsPageLocator.FLIGHT_PRICES).stream()
                .map(e -> e.getDomAttribute("data-price"))
                .filter(v -> v != null && !v.isBlank())
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDisplayedAirlines() {
        return driver.findElements(ClassicResultsPageLocator.FLIGHT_AIRLINES).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDisplayedOriginCodes() {
        return routeCodes(true);
    }

    @Override
    public List<String> getDisplayedDestinationCodes() {
        return routeCodes(false);
    }

    /**
     * Rota kutusundaki havalimanı kodlarını okur.
     * İlk kod kalkış, son kod varış havalimanıdır; aktarmalı uçuşlarda
     * aradaki duraklar göz ardı edilir.
     */
    private List<String> routeCodes(boolean origin) {
        List<String> result = new ArrayList<>();
        for (WebElement container : driver.findElements(ClassicResultsPageLocator.ROUTE_CONTAINERS)) {
            List<String> codes = container.findElements(ClassicResultsPageLocator.ROUTE_AIRPORT_ITEMS).stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!codes.isEmpty()) {
                result.add(origin ? codes.get(0) : codes.get(codes.size() - 1));
            }
        }
        return result;
    }

    @Override
    public int getDisplayedFlightCount() {
        return driver.findElements(ClassicResultsPageLocator.FLIGHT_ITEMS).size();
    }

    /**
     * Part 4: uçuş kartlarını yapılandırılmış kayıtlara dönüştürür.
     *
     * Klasik tasarımda her uçuş tek bir .flight-item kartında olduğu için
     * kart kart dolaşıp kartın İÇİNDEN okuyoruz - alanların birbirine karışma
     * riski yok.
     */
    @Override
    public List<FlightRecord> getFlightRecords() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.FLIGHT_ITEMS));

        List<FlightRecord> records = new ArrayList<>();
        for (WebElement card : driver.findElements(ClassicResultsPageLocator.FLIGHT_ITEMS)) {
            String price = textIn(card, ClassicResultsPageLocator.CARD_PRICE, "data-price");
            if (price == null || price.isBlank()) {
                continue;
            }
            records.add(new FlightRecord(
                    textIn(card, ClassicResultsPageLocator.CARD_AIRLINE, null),
                    textIn(card, ClassicResultsPageLocator.CARD_DEPARTURE_TIME, null),
                    textIn(card, ClassicResultsPageLocator.CARD_ARRIVAL_TIME, null),
                    textIn(card, ClassicResultsPageLocator.CARD_DURATION, null),
                    textIn(card, ClassicResultsPageLocator.FLIGHT_TRANSIT, null),
                    Double.parseDouble(price)));
        }
        return records;
    }

    /**
     * Kart içinden tek bir alanı okur. Alan yoksa null döner - tek bir eksik
     * alan yüzünden tüm kazıma işleminin çökmesini istemiyoruz.
     */
    private static String textIn(WebElement card, org.openqa.selenium.By locator, String attribute) {
        List<WebElement> found = card.findElements(locator);
        if (found.isEmpty()) {
            return null;
        }
        WebElement el = found.get(0);
        String value = attribute == null ? el.getText() : el.getDomAttribute(attribute);
        return value == null ? null : value.trim();
    }

    // ==================================================================
    // CASE 3 - Uçuş seçimi ve rezervasyona ilerleme
    // ==================================================================

    /**
     * İlk gidiş ve ilk dönüş uçuşunu seçip rezervasyon sayfasına ilerler.
     *
     * Akış canlı sitede adım adım doğrulandı; ayrıntısı
     * {@link locator.ClassicResultsPageLocator} içinde anlatılıyor. Dikkat
     * edilecek nokta: son adımda ayrı bir "ilerle" butonu YOK - dönüş paketine
     * tıklamak doğrudan rezervasyon sayfasına yönlendiriyor.
     */
    public CheckoutPage selectFirstDepartureAndReturnFlight() {
        // 1) Gidiş uçuşunu seç
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.DEPARTURE_FLIGHT_LIST));
        clickFirstSelectButtonIn(ClassicResultsPageLocator.DEPARTURE_FLIGHT_LIST, "gidiş");

        // 2) Açılan paketten "Seç ve İlerle"
        WebElement departurePackage = resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.OPENED_PACKAGE_WRAPPER));
        WebElement proceed = departurePackage.findElement(ClassicResultsPageLocator.PROVIDER_SELECT_BUTTON);
        scrollToElement(proceed);
        clickSafely(proceed);

        // 3) Dönüş listesi görünür olunca ilk dönüş uçuşunu seç
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.RETURN_FLIGHT_LIST));
        clickFirstSelectButtonIn(ClassicResultsPageLocator.RETURN_FLIGHT_LIST, "dönüş");

        // 4) Dönüş paketine tıkla -> rezervasyon sayfası
        WebElement returnPackage = resultsWait.until(d -> {
            WebElement list = d.findElement(ClassicResultsPageLocator.RETURN_FLIGHT_LIST);
            List<WebElement> items = list.findElements(ClassicResultsPageLocator.RETURN_PACKAGE_ITEM);
            return items.isEmpty() ? null : items.get(0);
        });
        scrollToElement(returnPackage);
        clickSafely(returnPackage);

        return new CheckoutPage(driver).waitUntilLoaded();
    }

    /**
     * Verilen listedeki ilk "Seç" butonuna tıklar ve paket panelinin AÇILDIĞINI
     * doğrular; açılmazsa elementi yeniden bulup tekrar dener.
     *
     * Neden yeniden bulma gerekiyor: sonuç listesi arka planda yeniden render
     * oluyor (fiyat güncellemeleri, "haftanın en ucuz bileti" gibi rozetler
     * sonradan ekleniyor). Bu sırada elde tuttuğumuz buton referansı kopuyor
     * ve tıklama sessizce hiçbir şey yapmıyor.
     */
    private void clickFirstSelectButtonIn(By listLocator, String leg) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement list = driver.findElement(listLocator);
                List<WebElement> buttons = list.findElements(ClassicResultsPageLocator.SELECT_FLIGHT_BUTTON);
                if (buttons.isEmpty()) {
                    throw new IllegalStateException(
                            leg + " listesinde tıklanabilir 'Seç' butonu bulunamadı. " +
                            "Liste boş olabilir (filtre çok dar) veya locator değişmiş olabilir.");
                }

                WebElement button = buttons.get(0);
                scrollToElement(button);
                clickSafely(button);

                // Paket paneli gerçekten açıldı mı? Açılmadıysa tıklama boşa gitmiştir.
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                        ExpectedConditions.visibilityOfElementLocated(
                                ClassicResultsPageLocator.OPENED_PACKAGE_WRAPPER));
                return;
            } catch (StaleElementReferenceException | TimeoutException retryable) {
                // Liste yeniden render olmuş; bir sonraki turda elementi yeniden buluyoruz.
            }
        }

        throw new IllegalStateException(
                leg + " listesinde 'Seç' butonuna tıklandı ancak paket paneli açılmadı " +
                "(3 deneme). Liste tıklama anında yeniden render oluyor olabilir.");
    }

    // ==================================================================
    // Yardımcılar
    // ==================================================================

    /** Akordeon kapalıysa açar. */
    /**
     * Filtre akordeonunu açar - ZATEN AÇIKSA DOKUNMAZ.
     *
     * BUG (bulundu): Bu metot önce durumu kontrol etmeden körlemesine tıklıyordu.
     * Akordeon o anda zaten açıksa tıklama onu KAPATIYOR, ardından beklenen
     * slider/checkbox elemanları hiç görünmüyor ve test "filtre paneli açılmadı"
     * diye zaman aşımına uğruyordu. Panelin bazen açık gelmesi tarayıcı ve
     * zamanlamaya göre değiştiği için hata yalnızca Firefox'ta ortaya çıkmıştı -
     * klasik bir "kör toggle" hatası.
     *
     * Bootstrap collapse kullanıldığı için açık durumun işareti net:
     * kapalıyken class="collapse", açıkken class="collapse show".
     */
    private void openAccordion(org.openqa.selenium.By header) {
        WebElement headerEl = resultsWait.until(ExpectedConditions.presenceOfElementLocated(header));
        scrollToElement(headerEl);

        if (isAccordionOpen(headerEl)) {
            return;
        }

        clickSafely(headerEl);

        // Açılma animasyonu bitene kadar bekle; aksi halde içerideki elemanları
        // henüz render olmadan aramaya başlıyoruz.
        resultsWait.until(d -> isAccordionOpen(d.findElement(header)));
    }

    private boolean isAccordionOpen(WebElement headerEl) {
        WebElement card = headerEl.findElement(org.openqa.selenium.By.xpath("ancestor::*[contains(@class,'filter-card')][1]"));
        return !card.findElements(org.openqa.selenium.By.cssSelector(".collapse.show")).isEmpty();
    }

    /**
     * Normal click engellenirse JS ile tıklar (overlay / pointer-events sorunları için).
     *
     * BUG (bulundu): Bu metot önce TÜM exception'ları yakalıyordu. Liste tıklamadan
     * hemen önce yeniden render olursa element "stale" olur; eski hâli bunu da
     * yakalayıp AYNI KOPMUŞ element üzerinde JS tıklaması yapıyordu - yani hiçbir
     * şey olmuyordu, üstelik sessizce. Artık sadece "tıklama engellendi" durumunu
     * yakalıyoruz; staleness çağırana yayılıyor ki elementi yeniden bulup
     * tekrar deneyebilsin.
     */
    private void clickSafely(WebElement element) {
        try {
            element.click();
        } catch (ElementNotInteractableException e) {   // ElementClickInterceptedException bunun alt sinifi
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    private static boolean isAscending(List<Double> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }
        return true;
    }
}
