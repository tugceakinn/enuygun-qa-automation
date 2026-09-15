package page;

import base.BasePage;
import locator.ClassicResultsPageLocator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
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
     * Handle gerçek bir element olduğu için dragAndDropBy ile merkezinden
     * yakalayıp yatay piksel farkı kadar taşıyoruz; bu, yeni tasarımdaki
     * native range input'a göre daha basit ama piksel hassasiyeti gerektirdiği
     * için sonucu doğrulayıp bir kez daha deniyoruz.
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
            new Actions(driver).dragAndDropBy(handle, deltaX, 0).perform();
        }

        int finalValue = readHandleMinutes(handle);
        // rc-slider adım büyüklüğü nedeniyle birkaç dakikalık sapma normal.
        if (Math.abs(finalValue - targetMinutes) > 15) {
            throw new IllegalStateException(
                    "Saat slider'ı hedefe ulaşmadı: istenen=" + targetMinutes +
                    " dk, gerçek=" + finalValue + " dk. Sürükleme piksel hesabı hatalı olabilir.");
        }
    }

    /** Filtre uygulandıktan sonra listenin güncellenmesini bekler (doğrulamayı test yapar). */
    private void waitForResultsToReflectFilter(int fromHour, int toHour) {
        try {
            resultsWait.until(d -> {
                List<LocalTime> times = getDisplayedDepartureTimes();
                if (times.isEmpty()) {
                    return false;
                }
                return times.stream().allMatch(t -> t.getHour() >= fromHour && t.getHour() <= toHour);
            });
        } catch (TimeoutException ignored) {
            // Kasıtlı: asıl doğrulamayı testteki assertion yapsın ki hata mesajı
            // "hangi uçuşlar aralık dışında" şeklinde anlamlı olsun.
        }
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
        try {
            resultsWait.until(d -> {
                List<String> airlines = getDisplayedAirlines();
                return !airlines.isEmpty()
                        && airlines.stream().allMatch(a -> a.equalsIgnoreCase(airlineName));
            });
        } catch (TimeoutException ignored) {
            // Doğrulamayı test katmanı yapsın.
        }
    }

    @Override
    public ClassicResultsPage sortByPriceAscending() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(
                ClassicResultsPageLocator.FLIGHT_ITEMS));

        WebElement sortButton = resultsWait.until(ExpectedConditions.elementToBeClickable(
                ClassicResultsPageLocator.SORT_PRICE_ASC));
        clickSafely(sortButton);

        try {
            resultsWait.until(d -> {
                List<Double> prices = getDisplayedPrices();
                return !prices.isEmpty() && isAscending(prices);
            });
        } catch (TimeoutException ignored) {
            // Doğrulamayı test katmanı yapsın.
        }
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

    // ==================================================================
    // Yardımcılar
    // ==================================================================

    /** Akordeon kapalıysa açar. */
    private void openAccordion(org.openqa.selenium.By header) {
        WebElement headerEl = resultsWait.until(ExpectedConditions.presenceOfElementLocated(header));
        scrollToElement(headerEl);
        clickSafely(headerEl);
    }

    /** Normal click engellenirse JS ile tıklar (overlay / pointer-events sorunları için). */
    private void clickSafely(WebElement element) {
        try {
            element.click();
        } catch (Exception e) {
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
