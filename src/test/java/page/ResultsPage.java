package page;

import base.BasePage;
import locator.ResultsPageLocator;
import org.openqa.selenium.By;
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
     * Onemli: burada TimeoutException'i bilincli olarak yutuyoruz. Amacimiz
     * testi "gecirmek" degil; sadece asenkron render'in tamamlanmasina sans
     * vermek. Liste gercekten filtreye uymuyorsa bekleme suresi dolar, metot
     * sessizce doner ve testteki asil assertion calisir - boylece hata mesaji
     * "hangi ucuslar araligin disinda kaldi" seklinde anlamli olur, teknik bir
     * TimeoutException yerine.
     */
    private void waitForResultsToReflectFilter(int fromHour, int toHour) {
        try {
            resultsWait.until(d -> {
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
        } catch (TimeoutException ignored) {
            // Kasitli: asil dogrulamayi test katmanindaki assertion yapsin.
        }
    }

    private int readSliderValue(WebElement rangeInput) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return Integer.parseInt(String.valueOf(js.executeScript("return arguments[0].value;", rangeInput)));
    }

    /**
     * BUG (bulundu): Selenium 3'te Actions.moveToElement(element, xOffset, yOffset)
     * offsetleri elementin MERKEZİNE göreydi; Selenium 4'ün W3C Actions API'sinde
     * ise bu offsetler elementin SOL-ÜST köşesine göre. Eski kod hâlâ merkeze göre
     * hesaplıyordu ("offsetFromCenter"), bu yüzden thumb track'in solunda/ortasındaysa
     * (küçük offset hatası) tıklama yine de thumb'ı yakalayabiliyordu, ama thumb en
     * sağ uçtaysa (örn. saat filtresinin "bitiş" slider'ı max'ta başlıyor) hesaplanan
     * nokta gerçek thumb'ın çok dışına düşüyor ve sürükleme hiçbir şey yapmıyordu
     * (bu yüzden "başlangıç saati" filtresi çalışıyor ama "bitiş saati" hiç
     * uygulanmıyordu, 23:59'da kalıyordu).
     * Şimdi elementin SOL-ÜST köşesine göre gerçek piksel x konumunu kullanıyoruz,
     * ve sürükleme sonrası gerçek değeri okuyup doğruluyoruz; tutmazsa bir kez daha
     * (güncel konumdan) deniyoruz, hâlâ tutmazsa sessizce yanlış filtrelenmiş sonuçla
     * devam etmek yerine net bir hata fırlatıyoruz.
     */
    private void setSliderValue(WebElement rangeInput, int targetValue) {
        for (int attempt = 0; attempt < 2; attempt++) {
            int min = Integer.parseInt(rangeInput.getDomAttribute("min"));
            int max = Integer.parseInt(rangeInput.getDomAttribute("max"));
            int current = readSliderValue(rangeInput);

            if (current == targetValue) {
                return;
            }

            int trackWidth = rangeInput.getSize().getWidth();
            int trackHeight = rangeInput.getSize().getHeight();
            double pxPerUnit = (double) (trackWidth - THUMB_WIDTH_PX) / (max - min);

            double currentThumbX = THUMB_WIDTH_PX / 2.0 + (current - min) * pxPerUnit;
            double targetThumbX = THUMB_WIDTH_PX / 2.0 + (targetValue - min) * pxPerUnit;

            int startX = (int) Math.round(currentThumbX);
            int targetX = (int) Math.round(targetThumbX);
            int y = Math.max(1, trackHeight / 2);

            new Actions(driver)
                    .moveToElement(rangeInput, startX, y)
                    .clickAndHold()
                    .moveToElement(rangeInput, targetX, y)
                    .release()
                    .perform();

            if (readSliderValue(rangeInput) == targetValue) {
                return;
            }
        }

        // Gercek surukleme tutmadi. Native range input'lar piksel hassasiyetine
        // cok bagimli (thumb genisligi, zoom, responsive track genisligi); bu
        // yuzden deterministik bir yedek yol kullaniyoruz.
        setValueViaNativeSetter(rangeInput, targetValue);

        int finalValue = readSliderValue(rangeInput);
        if (finalValue != targetValue) {
            throw new IllegalStateException(
                    "Slider hedef değere ulaşmadı: istenen=" + targetValue + ", gerçek=" + finalValue +
                    ". Hem sürükleme hem de programatik değer atama başarısız oldu; " +
                    "slider locator'ı yanlış elementi işaret ediyor olabilir.");
        }
    }

    /**
     * Slider degerini React ile uyumlu sekilde programatik olarak ayarlar.
     *
     * Neden bu kadar dolayli: React, kontrollu bir input'un ORNEK (instance)
     * uzerindeki "value" setter'ini kendi tracker'i ile eziyor. Dolayisiyla
     * klasik element.value = x atamasi React tarafindan "deger degismedi"
     * olarak gorulur ve ardindan gonderilen input event'i yok sayilir - slider
     * ekranda eski yerinde kalir.
     *
     * Cozum: PROTOTYPE uzerindeki orijinal (native) setter'i dogrudan cagirmak.
     * Bu, React'in instance-level tracker'ini atlar; sonrasinda bubbles=true
     * ile gonderilen 'input' ve 'change' event'leri React'in state'i gercekten
     * guncellemesini saglar.
     */
    private void setValueViaNativeSetter(WebElement rangeInput, int targetValue) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "const input = arguments[0];" +
                "const value = arguments[1];" +
                "const nativeSetter = Object.getOwnPropertyDescriptor(" +
                "    window.HTMLInputElement.prototype, 'value').set;" +
                "nativeSetter.call(input, value);" +
                "input.dispatchEvent(new Event('input',  { bubbles: true }));" +
                "input.dispatchEvent(new Event('change', { bubbles: true }));",
                rangeInput, String.valueOf(targetValue));
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
        try {
            resultsWait.until(d -> {
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
        } catch (TimeoutException ignored) {
            // Kasitli: dogrulamayi testteki assertion yapsin ki hata mesaji
            // "hangi havayollari kalmis" seklinde anlamli olsun.
        }
    }

    /** Sonuclari fiyata gore artan siralar ("En ucuz"). */
    public ResultsPage sortByPriceAscending() {
        resultsWait.until(ExpectedConditions.visibilityOfElementLocated(ResultsPageLocator.FLIGHT_DEPARTURE_TIMES));

        resultsWait.until(ExpectedConditions.elementToBeClickable(
                ResultsPageLocator.SORT_CHEAPEST_BUTTON)).click();

        // Siralama asenkron; liste gercekten sirali hale gelene kadar bekle.
        try {
            resultsWait.until(d -> {
                List<Double> now = getDisplayedPrices();
                return !now.isEmpty() && isAscending(now);
            });
        } catch (TimeoutException ignored) {
            // Dogrulamayi test katmani yapsin.
        }
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
