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
     * Takvim açıldığında hedef tarih görünen ayda olmayabilir. Görünmüyorsa
     * "ileri ay" butonuna basıp tekrar bakıyoruz; bulunca tıklıyoruz.
     * (Takvimde geri gitme butonu yok, geçmişe dönülemiyor.)
     */
    private void selectDateInCalendar(String fieldName, String date) {
        By container = HomePageLocator.datepickerContainer(fieldName);
        By dayLocator = HomePageLocator.dynamicDate(date);
        By forwardButton = HomePageLocator.monthForwardButton(fieldName);

        // Takvim popup'i acildiktan hemen sonra henuz render olmamis olabilir;
        // asil gun aramasina baslamadan once en az bir gun hucresi gelene
        // kadar bekliyoruz (aksi halde ilk kontrol yanlislikla "gorunmuyor"
        // deyip gereksiz yere ileri ay tusuna basabiliyordu).
        calendarWait.until(d -> !d.findElement(container)
                .findElements(HomePageLocator.ACTIVE_DAY_ANY).isEmpty());

        int[] forwardClicks = {0};
        calendarWait.until(d -> {
            WebElement containerEl = d.findElement(container);
            boolean visible = containerEl.findElements(dayLocator).stream().anyMatch(WebElement::isDisplayed);
            if (visible) {
                return true;
            }
            if (forwardClicks[0] >= MAX_MONTH_FORWARD_CLICKS) {
                // NoSuchElementException FluentWait tarafindan sessizce yutulup
                // tekrar denendigi icin BILEREK RuntimeException firlatiyoruz;
                // boylece 15-30sn boyunca yillarca ileri savrulmak yerine hemen
                // ve net bir mesajla basarisiz oluyor.
                throw new IllegalStateException(
                        "'" + date + "' tarihi '" + fieldName + "' takviminde " + MAX_MONTH_FORWARD_CLICKS +
                        " kez 'ileri ay' tusuna basilmasina ragmen bulunamadi. " +
                        "Tarih formatini (YYYY-MM-DD) ve datepicker-active-day/title locator'ini kontrol et.");
            }
            List<WebElement> forwardBtns = containerEl.findElements(forwardButton);
            if (!forwardBtns.isEmpty() && forwardBtns.get(0).isDisplayed()) {
                forwardBtns.get(0).click();
                forwardClicks[0]++;
            }
            return false;
        });

        driver.findElement(container).findElement(dayLocator).click();
    }

    public void clickSearchButton() {
        click(HomePageLocator.SEARCH_BUTTON);
    }
}
