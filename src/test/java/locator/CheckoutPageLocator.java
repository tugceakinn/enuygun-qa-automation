package locator;

import org.openqa.selenium.By;

public class CheckoutPageLocator {

    // İletişim Bilgileri (Contact Info)
    public static final By CONTACT_EMAIL_INPUT = By.cssSelector(
            "input[name='contact_email'], input[name='email'], input#contact_email, [data-testid='contact-email-input'], input[type='email']"
    );

    public static final By CONTACT_PHONE_INPUT = By.cssSelector(
            "input[name='contact_cellphone'], input[name='phone'], input#contact_cellphone, [data-testid='contact-phone-input'], input[type='tel']"
    );

    // Yolcu Bilgileri (Passenger Info - 1. Yolcu)
    public static final By FIRST_NAME_INPUT = By.cssSelector(
            "input[name='firstName_0'], input[name='firstName'], input#firstName_0, [data-testid='passenger-firstName-input'], input[name*='firstName']"
    );

    public static final By LAST_NAME_INPUT = By.cssSelector(
            "input[name='lastName_0'], input[name='lastName'], input#lastName_0, [data-testid='passenger-lastName-input'], input[name*='lastName']"
    );

    public static final By TC_ID_INPUT = By.cssSelector(
            "input[name='publicId_0'], input[name='publicId'], input#publicId_0, [data-testid='passenger-publicId-input'], input[name*='identityNumber'], input[name*='tcNumber']"
    );

    public static final By BIRTH_DATE_DAY = By.cssSelector(
            "select[name='birthDateDay_0'], select[name='birthDateDay'], select#birthDateDay_0, [data-testid='passenger-birthDateDay-select'], select[name*='Day']"
    );

    public static final By BIRTH_DATE_MONTH = By.cssSelector(
            "select[name='birthDateMonth_0'], select[name='birthDateMonth'], select#birthDateMonth_0, [data-testid='passenger-birthDateMonth-select'], select[name*='Month']"
    );

    public static final By BIRTH_DATE_YEAR = By.cssSelector(
            "select[name='birthDateYear_0'], select[name='birthDateYear'], select#birthDateYear_0, [data-testid='passenger-birthDateYear-select'], select[name*='Year']"
    );

    public static final By GENDER_MALE_LABEL = By.xpath(
            "//label[contains(@for, 'gender_M') or contains(@for, 'MALE') or contains(., 'Erkek')] | //span[contains(text(), 'Erkek')]/ancestor::label"
    );

    public static final By GENDER_FEMALE_LABEL = By.xpath(
            "//label[contains(@for, 'gender_F') or contains(@for, 'FEMALE') or contains(., 'Kadın')] | //span[contains(text(), 'Kadın')]/ancestor::label"
    );

    // Uçuş ve Fiyat Özeti
    public static final By FLIGHT_SUMMARY_BOX = By.cssSelector(
            ".flight-summary, .reservation-flight-detail, [data-testid='flight-summary'], .booking-summary, .summary-content, .selected-flight-info"
    );

    public static final By TOTAL_PRICE_TEXT = By.cssSelector(
            ".total-price, .price-detail, [data-testid='total-price'], .summary-price, .payment-total, .money-int"
    );

    // Ödeme / İlerleme Butonu
    public static final By PROCEED_PAYMENT_BUTTON = By.xpath(
            "//button[contains(., 'Ödemeye İlerle') or contains(., 'Ödemeye Geç') or contains(., 'Devam Et') or contains(., 'Kart Bilgileri')] | //*[@data-testid='proceed-payment-button']"
    );
}
