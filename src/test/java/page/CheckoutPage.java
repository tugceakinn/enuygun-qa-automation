package page;

import base.BasePage;
import locator.CheckoutPageLocator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class CheckoutPage extends BasePage {

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Ödeme / Rezervasyon (Checkout) sayfasının başarıyla yüklendiğini doğrular.
     */
    public boolean isCheckoutPageLoaded() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("odeme"),
                    ExpectedConditions.urlContains("rezervasyon"),
                    ExpectedConditions.urlContains("booking")
            ));
            return isElementDisplayed(CheckoutPageLocator.CONTACT_EMAIL_INPUT)
                    || isElementDisplayed(CheckoutPageLocator.FIRST_NAME_INPUT)
                    || isElementDisplayed(CheckoutPageLocator.FLIGHT_SUMMARY_BOX)
                    || isElementDisplayed(CheckoutPageLocator.PROCEED_PAYMENT_BUTTON);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * İletişim bilgileri form alanlarının görünür olduğunu doğrular.
     */
    public boolean isContactInfoSectionDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.CONTACT_EMAIL_INPUT)
                && isElementDisplayed(CheckoutPageLocator.CONTACT_PHONE_INPUT);
    }

    /**
     * Yolcu bilgileri form alanlarının görünür olduğunu doğrular.
     */
    public boolean isPassengerInfoSectionDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.FIRST_NAME_INPUT)
                && isElementDisplayed(CheckoutPageLocator.LAST_NAME_INPUT);
    }

    /**
     * Uçuş ve fiyat özeti alanının görünür olduğunu doğrular.
     */
    public boolean isFlightSummaryDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.FLIGHT_SUMMARY_BOX)
                || isElementDisplayed(CheckoutPageLocator.TOTAL_PRICE_TEXT);
    }

    /**
     * İletişim bilgilerini (E-posta ve Telefon) doldurur.
     */
    public CheckoutPage fillContactInfo(String email, String phone) {
        writeText(CheckoutPageLocator.CONTACT_EMAIL_INPUT, email);
        writeText(CheckoutPageLocator.CONTACT_PHONE_INPUT, phone);
        return this;
    }

    /**
     * 1. Yolcu bilgilerini (Ad, Soyad, TC Kimlik No, Doğum Tarihi, Cinsiyet) doldurur.
     */
    public CheckoutPage fillPassengerInfo(
            String firstName,
            String lastName,
            String tcId,
            String day,
            String month,
            String year,
            String gender) {

        writeText(CheckoutPageLocator.FIRST_NAME_INPUT, firstName);
        writeText(CheckoutPageLocator.LAST_NAME_INPUT, lastName);

        try {
            writeText(CheckoutPageLocator.TC_ID_INPUT, tcId);
        } catch (Exception ignored) {}

        // Doğum Tarihi Seçimi (Dropdown ise)
        try {
            List<WebElement> daySelects = findElements(CheckoutPageLocator.BIRTH_DATE_DAY);
            if (!daySelects.isEmpty()) {
                new Select(daySelects.get(0)).selectByValue(day);
            }
            List<WebElement> monthSelects = findElements(CheckoutPageLocator.BIRTH_DATE_MONTH);
            if (!monthSelects.isEmpty()) {
                new Select(monthSelects.get(0)).selectByValue(month);
            }
            List<WebElement> yearSelects = findElements(CheckoutPageLocator.BIRTH_DATE_YEAR);
            if (!yearSelects.isEmpty()) {
                new Select(yearSelects.get(0)).selectByValue(year);
            }
        } catch (Exception ignored) {}

        // Cinsiyet seçimi
        try {
            if ("Erkek".equalsIgnoreCase(gender) || "M".equalsIgnoreCase(gender)) {
                click(CheckoutPageLocator.GENDER_MALE_LABEL);
            } else {
                click(CheckoutPageLocator.GENDER_FEMALE_LABEL);
            }
        } catch (Exception ignored) {}

        return this;
    }

    /**
     * Ödeme / İlerleme butonunun görünür veya tıklanabilir olduğunu doğrular.
     */
    public boolean isProceedPaymentButtonDisplayed() {
        return isElementDisplayed(CheckoutPageLocator.PROCEED_PAYMENT_BUTTON);
    }
}
