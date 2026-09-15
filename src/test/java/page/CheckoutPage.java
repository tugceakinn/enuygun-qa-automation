package page;

import base.BasePage;
import locator.CheckoutPageLocator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Rezervasyon (checkout) sayfası.
 *
 * ÖNEMLİ SINIR: Bu sınıf gerçek bir satın alma akışı başlatmaz. "Ödemeye ilerle"
 * butonuna basılmaz; sadece butonun kullanılabilir olduğu doğrulanır. Formlar
 * bilinçli olarak AÇIKÇA SAHTE test verisiyle doldurulur.
 */
public class CheckoutPage extends BasePage {

    private final WebDriverWait checkoutWait;

    public CheckoutPage(WebDriver driver) {
        super(driver);
        this.checkoutWait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    /** Rezervasyon sayfasının yüklenmesini bekler. */
    public CheckoutPage waitUntilLoaded() {
        checkoutWait.until(ExpectedConditions.urlContains(CheckoutPageLocator.CHECKOUT_URL_PART));
        checkoutWait.until(ExpectedConditions.visibilityOfElementLocated(
                CheckoutPageLocator.FIRST_PASSENGER_SECTION));
        return this;
    }

    public boolean isCheckoutPageLoaded() {
        return driver.getCurrentUrl().contains(CheckoutPageLocator.CHECKOUT_URL_PART);
    }

    public boolean isContactInfoSectionDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.CONTACT_EMAIL_INPUT)
                && isElementDisplayed(CheckoutPageLocator.CONTACT_PHONE_INPUT);
    }

    public boolean isPassengerInfoSectionDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.FIRST_NAME_INPUT)
                && isElementDisplayed(CheckoutPageLocator.LAST_NAME_INPUT);
    }

    /** "Ödemeye ilerle" butonu görünür mü? (Basılmaz - bkz. sınıf yorumu.) */
    public boolean isProceedPaymentButtonDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.PROCEED_PAYMENT_BUTTON);
    }

    public CheckoutPage fillContactInfo(String email, String phone) {
        writeText(CheckoutPageLocator.CONTACT_EMAIL_INPUT, email);
        writeText(CheckoutPageLocator.CONTACT_PHONE_INPUT, phone);
        return this;
    }

    /**
     * 1. yolcunun bilgilerini doldurur.
     *
     * TC Kimlik No alanı BİLİNÇLİ OLARAK doldurulmuyor: sahte ama geçerli
     * görünen bir kimlik numarası üretmek doğru bir test verisi pratiği değil.
     * Kritik yol doğrulaması için gerekli de değil - formun kullanılabilir
     * olduğunu ad/soyad/doğum tarihi/cinsiyet alanları üzerinden doğruluyoruz.
     */
    public CheckoutPage fillPassengerInfo(String firstName, String lastName,
                                          String birthDay, String birthMonth, String birthYear,
                                          boolean male) {
        writeText(CheckoutPageLocator.FIRST_NAME_INPUT, firstName);
        writeText(CheckoutPageLocator.LAST_NAME_INPUT, lastName);

        // Gün ve ay seçeneklerinin value'ları SIFIR DOLGULU ("01", "02"...).
        // Çağıran tarafı bu ayrıntıya mecbur bırakmamak için burada normalize
        // ediyoruz; "1" de "01" de kabul ediliyor.
        new Select(checkoutWait.until(ExpectedConditions.visibilityOfElementLocated(
                CheckoutPageLocator.BIRTH_DATE_DAY))).selectByValue(padTwoDigits(birthDay));
        new Select(driver.findElement(CheckoutPageLocator.BIRTH_DATE_MONTH))
                .selectByValue(padTwoDigits(birthMonth));
        new Select(driver.findElement(CheckoutPageLocator.BIRTH_DATE_YEAR)).selectByValue(birthYear);

        // Radio'nun kendisi görsel olarak gizli olabildiği için label üzerinden tıklıyoruz
        clickGender(male);
        return this;
    }

    /** "1" -> "01" (yıl gibi 4 haneli değerlere dokunmaz). */
    private static String padTwoDigits(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    private void clickGender(boolean male) {
        String id = male ? "gender_M_0" : "gender_F_0";
        org.openqa.selenium.WebElement radio =
                driver.findElement(male ? CheckoutPageLocator.GENDER_MALE_RADIO
                                        : CheckoutPageLocator.GENDER_FEMALE_RADIO);
        if (radio.isSelected()) {
            return;
        }
        scrollToElement(radio);
        try {
            driver.findElement(org.openqa.selenium.By.cssSelector("label[for='" + id + "']")).click();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", radio);
        }
    }

    /** Seçilen yolcu bilgilerinin forma gerçekten yazıldığını doğrular. */
    public String getFirstNameValue() {
        return driver.findElement(CheckoutPageLocator.FIRST_NAME_INPUT).getDomProperty("value");
    }

    public String getLastNameValue() {
        return driver.findElement(CheckoutPageLocator.LAST_NAME_INPUT).getDomProperty("value");
    }

    public String getContactEmailValue() {
        return driver.findElement(CheckoutPageLocator.CONTACT_EMAIL_INPUT).getDomProperty("value");
    }
}
