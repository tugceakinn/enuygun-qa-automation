package locator;

import org.openqa.selenium.By;

/**
 * Rezervasyon (checkout) sayfası locator'ları.
 *
 * Bu locator'ların tamamı canlı sitede tek tek doğrulandı
 * (URL: /ucak-bileti/rezervasyon/detay/). Önceki sürümdeki tahmine dayalı
 * "olabilecek tüm isimleri virgülle deneyen" locator'lar kaldırıldı: o yaklaşım
 * yanlış elementi yakaladığında testi sessizce yanıltıyor, hata mesajını da
 * okunmaz hale getiriyordu.
 */
public class CheckoutPageLocator {

    /** Sayfanın kendisi: URL bu parçayı içerir. */
    public static final String CHECKOUT_URL_PART = "/rezervasyon/detay";

    // ---------------- İletişim bilgileri ----------------
    public static final By CONTACT_EMAIL_INPUT = By.id("contact_email");
    public static final By CONTACT_PHONE_INPUT = By.id("contact_cellphone");

    // ---------------- 1. yolcu bilgileri ----------------
    /** Yolcu formu kapsayıcısı (index 0 = ilk yetişkin yolcu). */
    public static final By FIRST_PASSENGER_SECTION = By.cssSelector(".row-passenger.passenger-0");

    public static final By FIRST_NAME_INPUT = By.id("firstName_0");
    public static final By LAST_NAME_INPUT = By.id("lastName_0");

    public static final By BIRTH_DATE_DAY = By.id("birthDateDay_0");
    public static final By BIRTH_DATE_MONTH = By.id("birthDateMonth_0");
    public static final By BIRTH_DATE_YEAR = By.id("birthDateYear_0");

    public static final By GENDER_MALE_RADIO = By.id("gender_M_0");
    public static final By GENDER_FEMALE_RADIO = By.id("gender_F_0");

    // ---------------- Uçuş / fiyat özeti ----------------
    public static final By FLIGHT_SUMMARY_BOX = By.cssSelector(".flight-summary, .reservation-flight-detail");

    // ---------------- Ödemeye ilerleme ----------------
    /**
     * DİKKAT: Bu butona test içinde BASILMAZ. Kritik yol testi, rezervasyon
     * sayfasına ulaşıldığını ve formun kullanılabilir olduğunu doğrulayıp durur;
     * gerçek bir ödeme akışı başlatmaz.
     */
    public static final By PROCEED_PAYMENT_BUTTON = By.id("continue-button");
}
