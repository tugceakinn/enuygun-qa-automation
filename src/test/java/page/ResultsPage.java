package page;

import base.BasePage;
import locator.ResultsPageLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ResultsPage extends BasePage implements FlightResults {

    // Slider thumb'ının CSS genişliği (tasarımda w-6/h-6 => 24px)
    private static final int THUMB_WIDTH_PX = 24;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Ucus arama sonuclari (ve sol taraftaki filtre paneli) bazen 15 sn'den
    // uzun surebiliyor; BasePage'deki genel 15sn'lik wait bu adim icin yetersiz
    // kalip TimeoutException firlatabiliyor. Bu yuzden sonuc sayfasina ozel
    // daha uzun bir wait kullaniyoruz.
    private final WebDriverWait resultsWait;

    public ResultsPage(WebDriver driver) {
        super(driver);
        this.resultsWait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    /**
     * Sol panelde "Gidiş Kalkış / varış saatleri" filtresini açar ve
     * kalkış saati aralığını (from-to) uygular.
     * Slider native bir &lt;input type="range" min=0 max=24 step=1&gt; olduğundan
     * ve klavye ile hareket ettirilemediğinden (site bunu engelliyor),
     * thumb'ı gerçek piksel konumuna sürükleyerek (drag) değeri değiştiriyoruz.
     */
    public ResultsPage applyDepartureTimeFilter(int fromHour, int toHour) {
        // Filtre panelini kullanmadan once en az bir ucus kartinin gercekten
        // render oldugundan emin ol - arama sonuclari akis halinde geldigi
        // icin filtre alani, veriler tamamen yuklenmeden gorunmeyebiliyor.
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES));

        resultsWait.until(ExpectedConditions.elementToBeClickable(ResultsPageLocator.TIME_FILTER_TOGGLE_BUTTON)).click();

        WebElement startInput = resultsWait.until(
                ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.DEPARTURE_TIME_SLIDER_START));
        WebElement endInput = driver.findElement(ResultsPageLocator.DEPARTURE_TIME_SLIDER_END);

        setSliderValue(startInput, fromHour);
        setSliderValue(endInput, toHour);

        // Slider degeri degistikten sonra ucus listesi ASENKRON olarak yeniden
        // render oluyor. Hemen okursak filtrelenmemis (eski) listeyi yakalayip
        // "filtre calismadi" gibi gorunen sahte bir hata aliriz. Bu yuzden
        // listenin filtreye uymasini bekliyoruz.
        waitForResultsToReflectFilter(fromHour, toHour);

        return this;
    }

    /**
     * Filtre uygulandiktan sonra sonuc listesinin guncellenmesini bekler.
     *
     * Onemli: PollingSupport.waitQuietly zaman asimini ve StaleElementReference
     * durumunu sessizce yutuyor. Amacimiz testi "gecirmek" degil; sadece
     * asenkron render'in tamamlanmasina sans vermek. Liste gercekten filtreye
     * uymuyorsa bekleme suresi dolar, metot sessizce doner ve testteki asil
     * assertion calisir - boylece hata mesaji "hangi ucuslar araligin disinda
     * kaldi" seklinde anlamli olur, teknik bir exception yerine.
     */
    private void waitForResultsToReflectFilter(int fromHour, int toHour) {
        PollingSupport.waitQuietly(resultsWait, d -> {
            List<WebElement> cells = d.findElements(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES);
            if (cells.isEmpty()) {
                return false;
            }
            for (WebElement cell : cells) {
                String text = cell.getText().trim();
                if (text.isEmpty()) {
                    return false;
                }
                int hour = LocalTime.parse(text, TIME_FORMAT).getHour();
                if (hour < fromHour || hour > toHour) {
                    return false;
                }
            }
            return true;
        });
    }

    private int readSliderValue(WebElement rangeInput) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return Integer.parseInt(String.valueOf(js.executeScript("return arguments[0].value;", rangeInput)));
    }

    /**
     * Slider'ı hedef değere sürükler.
     *
     * İKİ AYRI TUZAK VAR, ikisi de canlı sitede doğrulandı:
     *
     * 1) KOORDİNAT ÇERÇEVESİ: Selenium 4'ün W3C Actions API'sinde
     *    moveToElement(element, xOffset, yOffset) offsetleri elementin
     *    MERKEZİNE göredir (Selenium 3'teki JSON Wire Protocol'de sol-üst
     *    köşeye göreydi). Bu input'ta pointer-events:none olduğu ve SADECE
     *    ::-webkit-slider-thumb tıklanabilir olduğu için, yanlış çerçeveyle
     *    hesaplanan nokta thumb'ın dışına düşüyor, tıklama "içinden geçiyor"
     *    ve slider hiç kıpırdamıyor. Bu belirsizliğe hiç girmemek için
     *    aşağıda offsetli moveToElement KULLANMIYORUZ: önce offsetsiz
     *    moveToElement (= elementin merkezi, tartışmasız) sonra moveByOffset
     *    (= pointer'ın mevcut konumuna göre) kullanıyoruz.
     *
     * 2) TEK SIÇRAMA YETMİYOR: Sürükleme tek bir moveByOffset ile hedefe
     *    atlarsa, slider'ın DOM değeri doğru görünse bile sitenin filtreleme
     *    mantığı TETİKLENMİYOR (liste filtrelenmemiş kalıyor). Gerçek bir
     *    kullanıcı sürüklemesindeki gibi ARDIŞIK mousemove olayları gerekiyor,
     *    bu yüzden hedefe 10 küçük adımda ilerliyoruz.
     *
     * (Daha önce burada "native setter ile programatik değer atama" şeklinde
     * bir yedek yol vardı. O yol input.value'yu doğru değere getirdiği için
     * doğrulama testi geçiyordu, ama filtrelemeyi hiç tetiklemediğinden
     * "slider doğru görünüyor ama liste yanlış" gibi kafa karıştırıcı bir
     * duruma yol açıyordu - yani hatayı düzeltmiyor, gizliyordu. Kaldırıldı.)
     */
    private void setSliderValue(WebElement rangeInput, int targetValue) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int min = Integer.parseInt(rangeInput.getDomAttribute("min"));
            int max = Integer.parseInt(rangeInput.getDomAttribute("max"));
            int current = readSliderValue(rangeInput);

            if (current == targetValue) {
                return;
            }

            int trackWidth = rangeInput.getSize().getWidth();
            double pxPerUnit = (double) (trackWidth - THUMB_WIDTH_PX) / (max - min);

            // Thumb merkezlerinin, track'in SOL kenarına göre piksel konumu
            double currentThumbX = THUMB_WIDTH_PX / 2.0 + (current - min) * pxPerUnit;
            double targetThumbX = THUMB_WIDTH_PX / 2.0 + (targetValue - min) * pxPerUnit;

            // Pointer'ı elementin merkezinden alıp thumb'a taşıyacak offset
            double centerX = trackWidth / 2.0;
            int grabOffsetFromCenter = (int) Math.round(currentThumbX - centerX);
            int dragDistance = (int) Math.round(targetThumbX - currentThumbX);

            dragThumb(rangeInput, grabOffsetFromCenter, dragDistance);

            if (readSliderValue(rangeInput) == targetValue) {
                return;
            }
        }

        int finalValue = readSliderValue(rangeInput);
        throw new IllegalStateException(
                "Slider hedef değere ulaşmadı: istenen=" + targetValue + ", gerçek=" + finalValue +
                ". Sürükleme koordinat hesaplaması veya track genişliği hatalı olabilir.");
    }

    /**
     * Thumb'ı yakalayıp yatayda dragDistance kadar, TEK sıçrama yerine birden
     * fazla küçük ara adımla sürükler.
     *
     * Offsetli moveToElement bilinçli olarak kullanılmıyor (bkz. setSliderValue
     * yorumu, 1. tuzak): önce offsetsiz moveToElement ile elementin merkezine
     * gidiyor, sonra moveByOffset ile pointer'ı thumb'ın üstüne taşıyoruz.
     */
    private void dragThumb(WebElement rangeInput, int grabOffsetFromCenter, int dragDistance) {
        Actions actions = new Actions(driver);
        actions.moveToElement(rangeInput)
                .moveByOffset(grabOffsetFromCenter, 0)
                .clickAndHold();

        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            int stepDelta = (dragDistance * i / steps) - (dragDistance * (i - 1) / steps);
            actions.moveByOffset(stepDelta, 0);
        }

        actions.release().perform();
    }

    // ==================================================================
    // CASE 2 - Havayolu filtresi + fiyata gore artan siralama
    // ==================================================================

    /**
     * Sol paneldeki "Havayollari" filtresinden SADECE verilen havayolunu secer.
     *
     * Checkbox'lar "dahil et" mantiginda calisiyor. Bu yuzden once istenen
     * havayolunu isaretliyor, sonra digerlerinin isaretini kaldiriyoruz.
     * Sirasi onemli: once hepsini kaldirip sonra secmeye calisirsak site
     * "hic filtre yok" durumuna dusup tum listeyi geri getirebiliyor.
     */
    public ResultsPage filterByAirlineOnly(String airlineName) {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES));

        WebElement section = resultsWait.until(
                ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.AIRLINES_FILTER_SECTION));
        scrollToElement(section);

        // Bolum kapaliysa ac (checkbox'lar gorunur degilse)
        if (driver.findElements(ResultsPageLocator.AIRLINE_CHECKBOXES).isEmpty()) {
            resultsWait.until(ExpectedConditions.elementToBeClickable(
                    ResultsPageLocator.AIRLINES_FILTER_TOGGLE)).click();
        }

        List<WebElement> boxes = resultsWait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(ResultsPageLocator.AIRLINE_CHECKBOXES));

        if (boxes.isEmpty()) {
            throw new IllegalStateException(
                    "Havayollari filtresinde hic checkbox bulunamadi. " +
                    "Filtre bolumu acilmamis veya locator degismis olabilir.");
        }

        WebElement targetLabel = driver.findElements(
                ResultsPageLocator.airlineCheckboxByName(airlineName)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "'" + airlineName + "' havayolu filtre listesinde bulunamadi. " +
                        "Bu rotada bu havayolunun ucusu olmayabilir."));

        // 1) Hedef havayolunu isaretle
        if (!isCheckboxSelected(targetLabel)) {
            clickLabel(targetLabel);
        }

        // 2) Digerlerinin isaretini kaldir
        for (WebElement box : driver.findElements(ResultsPageLocator.AIRLINE_CHECKBOXES)) {
            WebElement label = box.findElement(By.xpath("ancestor::label[1]"));
            boolean isTarget = label.getText().contains(airlineName);
            if (!isTarget && box.isSelected()) {
                clickLabel(label);
            }
        }

        waitForAirlineFilterToApply(airlineName);
        return this;
    }

    private boolean isCheckboxSelected(WebElement label) {
        List<WebElement> inputs = label.findElements(By.cssSelector("input[type='checkbox']"));
        return !inputs.isEmpty() && inputs.get(0).isSelected();
    }

    /**
     * Label'a JS ile tikliyoruz: gercek <input> uzerinde pointer-events kapali
     * olabiliyor ve normal click "element click intercepted" hatasi veriyor.
     */
    private void clickLabel(WebElement label) {
        scrollToElement(label);
        try {
            label.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", label);
        }
    }

    /** Filtre uygulandiktan sonra listenin gercekten guncellenmesini bekler. */
    private void waitForAirlineFilterToApply(String airlineName) {
        PollingSupport.waitQuietly(resultsWait, d -> {
            List<WebElement> logos = d.findElements(ResultsPageLocator.FLIGHT_AIRLINE_LOGOS);
            if (logos.isEmpty()) {
                return false;
            }
            for (WebElement logo : logos) {
                String alt = logo.getDomAttribute("alt");
                if (alt == null || !alt.trim().equalsIgnoreCase(airlineName)) {
                    return false;
                }
            }
            return true;
        });
    }

    /** Sonuclari fiyata gore artan siralar ("En ucuz"). */
    public ResultsPage sortByPriceAscending() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES));

        resultsWait.until(ExpectedConditions.elementToBeClickable(
                ResultsPageLocator.SORT_CHEAPEST_BUTTON)).click();

        // Siralama asenkron; liste gercekten sirali hale gelene kadar bekle.
        PollingSupport.waitQuietly(resultsWait, d -> {
            List<Double> now = getDisplayedPrices();
            return !now.isEmpty() && isAscending(now);
        });
        return this;
    }

    /**
     * Listelenen fiyatlari sayisal olarak doner.
     * Gorunen metin ("1.486 TL") yerine data-price="1485.99" attribute'unu
     * okuyoruz; boylece binlik ayraci ve para birimi parse etmek gerekmiyor.
     */
    public List<Double> getDisplayedPrices() {
        return driver.findElements(ResultsPageLocator.FLIGHT_PRICES).stream()
                .map(e -> e.getDomAttribute("data-price"))
                .filter(v -> v != null && !v.isBlank())
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    /** Listelenen ucuslarin havayolu adlarini doner (logo img alt metninden). */
    public List<String> getDisplayedAirlines() {
        return driver.findElements(ResultsPageLocator.FLIGHT_AIRLINE_LOGOS).stream()
                .map(e -> e.getDomAttribute("alt"))
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static boolean isAscending(List<Double> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

    /** Ekranda listelenen tüm uçuşların kalkış saatlerini LocalTime olarak döner. */
    public List<LocalTime> getDisplayedDepartureTimes() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES));
        return driver.findElements(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES).stream()
                .map(WebElement::getText)
                .map(text -> LocalTime.parse(text.trim(), TIME_FORMAT))
                .collect(Collectors.toList());
    }

    public List<String> getDisplayedOriginCodes() {
        return driver.findElements(ResultsPageLocator.FLIGHT_ORIGIN_CODES).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public List<String> getDisplayedDestinationCodes() {
        return driver.findElements(ResultsPageLocator.FLIGHT_DESTINATION_CODES).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public int getDisplayedFlightCount() {
        return driver.findElements(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES).size();
    }
}
