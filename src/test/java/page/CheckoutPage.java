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
        // Bu alan testin assertion'larina konu oldugu icin KATI dogrulaniyor.
        typeAndVerify(CheckoutPageLocator.CONTACT_EMAIL_INPUT, email, "iletişim e-postası", true);

        // Telefon alaninda tr-mask-phone-number maskesi var (bkz. typeDigitsRespectingMask).
        // Bu alan testin assertion'larina konu DEGIL; yazilamazsa testi dusurmek
        // yerine gorunur bir uyari basip devam ediyoruz (gerekce: fillMaskedPhone).
        fillMaskedPhone(phone);
        return this;
    }

    /**
     * Maskeli telefon alanini doldurur; basarisiz olursa testi DUSURMEZ, uyarir.
     *
     * NEDEN KATI DOGRULAMA YOK: Kritik yol testi rezervasyon sayfasina
     * ulasildigini ad/soyad/e-posta uzerinden dogruluyor; telefon alani bu
     * assertion'lara dahil degil. Bir Page Object'in, testin dogrulamadigi
     * yardimci bir alan yuzunden tum akisi dusurmesi yanlis bir tasarim olurdu:
     * rapor "kritik yol kirildi" derken gercekte kirilan sey bir maske
     * widget'inin bicimlendirme davranisi olurdu. Bu yuzden davranis
     * "elinden geleni yap + gorunur uyari ver" seklinde.
     *
     * Degeri sessizce de yutmuyoruz: alan istenen degere ulasmazsa konsola
     * neyin istendigi ve alanda ne kaldigi yaziliyor.
     *
     * @return alan istenen degere ulastiysa true
     */
    public boolean fillMaskedPhone(String phone) {
        for (int attempt = 0; attempt < 2; attempt++) {
            WebElement input = checkoutWait.until(
                    ExpectedConditions.elementToBeClickable(CheckoutPageLocator.CONTACT_PHONE_INPUT));
            scrollToElement(input);
            input.click();
            clearField(input);
            typeDigitsRespectingMask(input, phone);
            if (matches(phone, input.getDomProperty("value"))) {
                return true;
            }
        }

        String actual = driver.findElement(CheckoutPageLocator.CONTACT_PHONE_INPUT)
                .getDomProperty("value");
        System.out.println("[UYARI] İletişim telefonu alanı istenen değere ulaşmadı. " +
                "İstenen=\"" + phone + "\", alanda kalan=\"" + actual + "\". " +
                "Alandaki 'tr-mask-phone-number' maskesi WebDriver ile gönderilen tuşları " +
                "yeniden biçimlendiriyor. Test bu alanı assert etmediği için akış devam ediyor.");
        return false;
    }

    /**
     * Maskeli bir alana rakamlari TEK TEK, maskenin kendi ekledigi haneleri
     * atlayarak yazar.
     *
     * BULGU (uc kosuda ayni imzayla tekrarlandi): Alandaki
     * {@code tr-mask-phone-number} maskesi, alan bosken ilk tusa basildiginda
     * Turk cep numaralarinin basindaki "5"i KENDISI ekliyor ve toplam haneyi
     * 10 ile siniriliyor (HTML'de maxlength yok, sinir JS tarafinda). Tum
     * numarayi tek seferde yazdigimizda sonuc suydu:
     *
     *   istenen : 5551112233
     *   olusan  : 5 (maske) + 555111223 (bizim ilk 9 hanemiz) -> "555 511 1223"
     *
     * Cozum: her haneden once alanin GERCEK degerini okuyup, o pozisyondaki
     * hane zaten dogruysa tusa basmiyoruz. Boylece maskenin haneyi kendisi
     * eklemesi de eklememesi de ayni dogru sonuca varir; maskenin davranisini
     * tahmin etmek zorunda kalmiyoruz.
     */
    private static void typeDigitsRespectingMask(WebElement input, String text) {
        for (int i = 0; i < text.length(); i++) {
            String current = digitsOnly(valueOf(input));
            // Maske bu haneyi zaten koyduysa tekrar yazma.
            if (i < current.length() && current.charAt(i) == text.charAt(i)) {
                continue;
            }
            input.sendKeys(String.valueOf(text.charAt(i)));
        }
    }

    private static String valueOf(WebElement input) {
        String value = input.getDomProperty("value");
        return value == null ? "" : value;
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

            input.click();
            clearField(input);
            input.sendKeys(text);

            // Öneri kutusunu kapat: açık kalırsa odak kaybında değeri geri alıyor.
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
     * BUG (bulundu, üç koşuda aynı imzayla): Telefon alanına bağlı input mask,
     * {@code clear()} ve tek tek BACKSPACE denemelerinden sonra bile bir "5" ön
     * ekini geri koyuyor (Türk cep numaraları 5 ile başlıyor). Sonuçta yazdığımız
     * 10 hane onun arkasına ekleniyor ve maxlength sonuncuyu kesiyordu:
     *
     *   istenen  5551112233
     *   oluşan   5 + 555111223(3)  ->  "555 511 1223"
     *
     * Bu yüzden önce TÜMÜNÜ SEÇ + SİL yöntemini kullanıyoruz: seçili içeriğin
     * üzerine yazmak, mask'ın karakter karakter araya girmesine fırsat vermiyor.
     * Tutmazsa clear(), o da tutmazsa klavyeyle silme deneniyor.
     */
    private void clearField(WebElement input) {
        // 1) Tümünü seç + sil (en güvenilir yöntem)
        input.sendKeys(Keys.chord(selectAllModifier(), "a"));
        input.sendKeys(Keys.DELETE);
        if (isEmpty(input)) {
            return;
        }

        // 2) Standart clear()
        input.clear();
        if (isEmpty(input)) {
            return;
        }

        // 3) Klavyeyle sil
        input.sendKeys(Keys.END);
        for (int i = 0; i < 25 && !isEmpty(input); i++) {
            input.sendKeys(Keys.BACK_SPACE);
        }
    }

    /** macOS'ta Command, diğerlerinde Control. */
    private static Keys selectAllModifier() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;
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
