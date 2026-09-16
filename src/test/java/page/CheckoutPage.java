package page;

import base.BasePage;
import locator.CheckoutPageLocator;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
        // E-posta alanina jQuery UI autocomplete bagli -> oneri kutusunu kapatmak gerekiyor.
        typeAndVerify(CheckoutPageLocator.CONTACT_EMAIL_INPUT, email, "iletişim e-postası", true);
        // Telefon alaninda input MASK var -> ESCAPE gonderilmemeli (mask'i sifirliyor).
        typeAndVerify(CheckoutPageLocator.CONTACT_PHONE_INPUT, phone, "iletişim telefonu", false);
        return this;
    }

    /**
     * Bir alana yazar ve YAZILDIĞINI DOĞRULAR; tutmazsa bir kez daha dener.
     *
     * BUG (bulundu): İletişim e-postası alanı Firefox'ta boş kalıyordu (Chrome'da
     * sorun yoktu). Sebep, alana bağlı jQuery UI autocomplete widget'ı:
     * alan {@code class="... ui-autocomplete-input"} taşıyor ve yazarken açılan
     * öneri kutusu, odak kaybında değeri geri alabiliyor. Ad/soyad alanlarında
     * bu widget olmadığı için onlar sorunsuz yazılıyordu.
     *
     * Bu yüzden yazdıktan sonra ESCAPE ile öneri kutusunu kapatıyor, sonra
     * alanın GERÇEK değerini (DOM property) okuyup doğruluyoruz. Sessizce boş
     * geçmek yerine, hangi alanın yazılamadığını söyleyen net bir hata veriyoruz.
     */
    private void typeAndVerify(org.openqa.selenium.By locator, String text, String fieldName,
                               boolean dismissSuggestions) {
        for (int attempt = 0; attempt < 2; attempt++) {
            WebElement input = checkoutWait.until(
                    ExpectedConditions.elementToBeClickable(locator));
            scrollToElement(input);
            clearField(input);
            input.sendKeys(text);

            // Sadece autocomplete'li alanda öneri kutusunu kapat. Maskeli alanlarda
            // (telefon) ESCAPE mask tarafından "geri al" olarak yorumlanıp alanı
            // temizliyor - bu yüzden koşullu.
            if (dismissSuggestions) {
                input.sendKeys(Keys.ESCAPE);
            }

            if (matches(text, input.getDomProperty("value"))) {
                return;
            }
        }

        String actual = driver.findElement(locator).getDomProperty("value");
        throw new IllegalStateException(
                "'" + fieldName + "' alanına yazılamadı. İstenen=\"" + text +
                "\", alanda kalan=\"" + actual + "\". Alana bağlı bir autocomplete, " +
                "mask veya doğrulama widget'ı değeri geri almış olabilir.");
    }

    /**
     * Alanı gerçekten boşaltır.
     *
     * BUG (bulundu, hata mesajından teşhis edildi): Telefon alanında
     * {@code clear()} Firefox'ta işe yaramıyordu - alana bağlı input mask
     * değeri (Türk cep numaraları 5 ile başladığı için muhtemelen bir "5" ön eki)
     * hemen geri koyuyor. Bizim yazdığımız 10 hane bunun ARKASINA ekleniyor,
     * maxlength son haneyi kesiyordu:
     *
     *   istenen  5551112233
     *   oluşan   5 + 555111223(3) -> "555 511 1223"
     *
     * Bu yüzden clear() yetmiyorsa klavyeyle (END + BACKSPACE) gerçekten
     * boşaltıyoruz.
     */
    private void clearField(WebElement input) {
        input.clear();
        if (isEmpty(input)) {
            return;
        }

        input.click();
        input.sendKeys(Keys.END);
        // Alanda kalan karakter sayısından biraz fazlasını sil (mask karakterleri dahil).
        for (int i = 0; i < 25 && !isEmpty(input); i++) {
            input.sendKeys(Keys.BACK_SPACE);
        }
    }

    private static boolean isEmpty(WebElement input) {
        String value = input.getDomProperty("value");
        return value == null || value.isEmpty();
    }

    /**
     * Girilen değerin alana işlendiğini doğrular.
     *
     * Maskeli alanlar değeri BİÇİMLENDİRİR: "5551112233" yazdığımızda alanda
     * "(555) 111 22 33" durabilir. Bu yüzden yalnızca rakamlardan oluşan
     * değerleri rakam bazında karşılaştırıyoruz - aksi halde alan doğru
     * doldurulmuş olmasına rağmen doğrulama başarısız olurdu.
     */
    private static boolean matches(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (!expected.isEmpty() && expected.chars().allMatch(Character::isDigit)) {
            return digitsOnly(expected).equals(digitsOnly(actual));
        }
        return false;
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
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
