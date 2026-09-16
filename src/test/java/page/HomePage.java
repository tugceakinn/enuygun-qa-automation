package page;

import base.BasePage;
import locator.HomePageLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class HomePage extends BasePage {

    // Takvim popup'i icin ayri, biraz daha uzun bir wait + "ileri ay" tusuna
    // sonsuz basmayi engelleyen bir güvenlik sınırı. Hedef tarih normalde
    // görünen ilk ayda (ya da bir-iki ay ilerisinde) olur; bu sınırın çok
    // üzerine çıkılması locator/format sorununa işaret eder, sessizce yıllar
    // sonrasına savrulmak yerine burada net bir hata fırlatıyoruz.
    private static final int MAX_MONTH_FORWARD_CLICKS = 14;
    private final WebDriverWait calendarWait;

    public HomePage(WebDriver driver) {
        super(driver);
        this.calendarWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public HomePage goToEnuygun() {
        driver.get("https://www.enuygun.com/");
        return this;
    }

    public HomePage acceptCookies() {
        try {
            click(HomePageLocator.ACCEPT_COOKIES);
        } catch (Exception e) {
            System.out.println("Çerez banner'ı çıkmadı, teste devam ediliyor.");
        }
        return this;
    }

    public HomePage selectRoundTrip() {
        click(HomePageLocator.ROUND_TRIP_RADIO);
        return this;
    }

    public HomePage selectOrigin(String originCity) {
        selectCity(HomePageLocator.ORIGIN_CONTAINER, HomePageLocator.ORIGIN_INPUT, originCity);
        return this;
    }

    public HomePage selectDestination(String destinationCity) {
        selectCity(HomePageLocator.DESTINATION_CONTAINER, HomePageLocator.DESTINATION_INPUT, destinationCity);
        return this;
    }

    /**
     * Kutuya tıklanınca site zaten "Popüler Şehirler" listesini varsayılan
     * olarak gösteriyor (İstanbul, Ankara, İzmir... gibi sık kullanılan şehirler
     * yazmaya gerek kalmadan orada hazır duruyor). Bu yüzden önce hiç yazmadan
     * doğrudan o listede şehri arıyoruz — en güvenilir yol bu, çünkü sendKeys
     * ile yazılan metnin sitenin filtre state'ini her zaman tetiklemediğini
     * gördük. Şehir popüler listede yoksa (parametrik/az bilinen bir şehirse)
     * input'a yazıp filtrelenmiş listeyi bekliyoruz.
     */
    private void selectCity(By containerLocator, By inputLocator, String cityName) {
        WebElement container = wait.until(ExpectedConditions.elementToBeClickable(containerLocator));
        container.click();

        By suggestionLocator = HomePageLocator.suggestionByCityName(cityName);

        WebElement suggestion = findSuggestion(containerLocator, suggestionLocator, Duration.ofSeconds(3));

        if (suggestion == null) {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            input.click();
            input.sendKeys(cityName);
            suggestion = findSuggestion(containerLocator, suggestionLocator, Duration.ofSeconds(10));
        }

        if (suggestion == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "'" + cityName + "' için öneri listesinde eşleşme bulunamadı.");
        }

        suggestion.click();
    }

    private WebElement findSuggestion(By containerLocator, By suggestionLocator, Duration timeout) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, timeout);
            return shortWait.until(d -> {
                WebElement freshContainer = d.findElement(containerLocator);
                List<WebElement> matches = freshContainer.findElements(suggestionLocator);
                for (WebElement el : matches) {
                    if (el.isDisplayed()) {
                        return el;
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            return null;
        }
    }

    public HomePage selectDates(String departureDate, String returnDate) {
        click(HomePageLocator.DEPARTURE_DATE);
        selectDateInCalendar("departureDate", departureDate);

        // Gidiş seçilince dönüş takvimi otomatik kapanabilir / farklı bir gün
        // atanmış olabilir; dönüş kutusuna tıklayıp istediğimiz tarihi bilerek seçiyoruz.
        click(HomePageLocator.RETURN_DATE);
        selectDateInCalendar("returnDate", returnDate);

        return this;
    }

    /**
     * Takvimde hedef tarihi bulup tıklar; görünen ayda değilse "ileri ay"
     * butonuna basarak ilerler. (Takvimde geri gitme butonu yok.)
     *
     * BUG (bulundu, hata dökümünden doğrulandı): Bu metot önce gün aramasını
     * ilgili alanın datepicker KAPSAYICISINA kapsıyordu. Ama takvim günleri her
     * zaman o kapsayıcının içinde render edilmiyor (popup ayrı bir katmana
     * taşınabiliyor); ileri-ay butonu ise kapsayıcının içinde. Sonuç: gün hiç
     * bulunamıyor, buton her turda tıklanıyor ve takvim aylarca ileri kayıyordu
     * - rezervasyon aralığının sonuna varıp tüm günler "passive" olunca da
     * TimeoutException'la patlıyordu.
     *
     * Çözüm: aynı anda yalnızca bir takvim açık olduğu için günü SAYFA GENELİNDE
     * ve GÖRÜNÜRLÜK şartıyla arıyoruz; ileri-ay butonu ise alana kapsalı kalıyor
     * (çünkü o testid gerçekten alana özgü).
     *
     * Ayrıca "ilerleme var mı" kontrolü eklendi: ileri tıklamaya rağmen görünen
     * ilk tarih değişmiyorsa takvim takılmış demektir, üst sınırı beklemeden
     * hemen ve net bir mesajla başarısız oluyoruz.
     */
    private void selectDateInCalendar(String fieldName, String date) {
        By dayLocator = HomePageLocator.dynamicDate(date);
        By forwardButton = HomePageLocator.monthForwardButton(fieldName);

        // Takvim açıldıktan hemen sonra henüz render olmamış olabilir; gün
        // aramasına başlamadan önce en az bir gün hücresi gelsin.
        calendarWait.until(d -> d.findElements(HomePageLocator.ACTIVE_DAY_ANY).stream()
                .anyMatch(WebElement::isDisplayed));

        int[] forwardClicks = {0};
        String[] lastFirstDate = {null};

        calendarWait.until(d -> {
            if (isDisplayed(d.findElements(dayLocator))) {
                return true;
            }

            String firstDate = firstVisibleDayTitle(d);

            if (forwardClicks[0] > 0 && firstDate != null && firstDate.equals(lastFirstDate[0])) {
                throw new IllegalStateException(
                        "'" + fieldName + "' takviminde 'ileri ay' tuşuna basıldı ama görünen ay " +
                        "değişmedi (hâlâ " + firstDate + "). Takvim ilerlemiyor; " +
                        "ileri-ay butonu locator'ı yanlış elementi işaret ediyor olabilir.");
            }
            lastFirstDate[0] = firstDate;

            if (forwardClicks[0] >= MAX_MONTH_FORWARD_CLICKS) {
                // NoSuchElementException FluentWait tarafından sessizce yutulup
                // tekrar denendiği için BİLEREK IllegalStateException fırlatıyoruz.
                throw new IllegalStateException(
                        "'" + date + "' tarihi '" + fieldName + "' takviminde " + MAX_MONTH_FORWARD_CLICKS +
                        " kez 'ileri ay' tuşuna basılmasına rağmen bulunamadı. " +
                        "Görünen ilk tarih: " + firstDate + ". Tarih geçmişte veya rezervasyon " +
                        "aralığının dışında olabilir (o günler 'datepicker-passive-day' olur).");
            }

            List<WebElement> forwardBtns = d.findElements(forwardButton);
            if (!forwardBtns.isEmpty() && forwardBtns.get(0).isDisplayed()) {
                forwardBtns.get(0).click();
                forwardClicks[0]++;
            }
            return false;
        });

        driver.findElements(dayLocator).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "'" + date + "' tarihi bulundu ama tıklanmadan önce görünmez oldu."))
                .click();
    }

    private static boolean isDisplayed(List<WebElement> elements) {
        return elements.stream().anyMatch(WebElement::isDisplayed);
    }

    /** Ekranda görünen ilk takvim gününün title'ı (ilerleme kontrolü için). */
    private static String firstVisibleDayTitle(WebDriver d) {
        return d.findElements(HomePageLocator.ANY_DAY)
                .stream()
                .filter(WebElement::isDisplayed)
                .map(e -> e.getDomAttribute("title"))
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .orElse(null);
    }

    public void clickSearchButton() {
        ensureHotelListingUnchecked();
        click(HomePageLocator.SEARCH_BUTTON);
    }

    /**
     * "Bu tarihler için otelleri de listele" kutusunun işaretini kaldırır.
     *
     * BUG (bulundu, hata dökümünden teşhis edildi): Bu kutu sitede varsayılan
     * olarak İŞARETLİ gelebiliyor. İşaretliyken arama uçuş sonuç sayfası yerine
     * otel sayfasına sapıyordu - hata dökümünde açılan sayfanın "Ankara Otelleri"
     * olduğu görüldü ve varyant tespiti doğal olarak hiçbir uçuş işareti bulamadı.
     *
     * Kutu her varyantta bulunmuyor; yoksa sessizce geçiyoruz (bu, bir hatayı
     * yutmak değil - kutunun yokluğu zaten istediğimiz durum).
     */
    private void ensureHotelListingUnchecked() {
        List<WebElement> checkboxes = driver.findElements(HomePageLocator.HOTEL_LISTING_CHECKBOX);
        if (checkboxes.isEmpty() || !checkboxes.get(0).isSelected()) {
            return;
        }

        // input görsel olarak gizli olabildiği için label üzerinden tıklıyoruz.
        List<WebElement> labels = driver.findElements(HomePageLocator.HOTEL_LISTING_LABEL);
        if (labels.isEmpty()) {
            throw new IllegalStateException(
                    "'Otelleri de listele' kutusu işaretli ama kaldırmak için label bulunamadı.");
        }
        scrollToElement(labels.get(0));
        labels.get(0).click();

        wait.until(d -> {
            List<WebElement> boxes = d.findElements(HomePageLocator.HOTEL_LISTING_CHECKBOX);
            return boxes.isEmpty() || !boxes.get(0).isSelected();
        });
    }
}
