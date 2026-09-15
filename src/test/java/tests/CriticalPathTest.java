package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import page.CheckoutPage;
import page.ClassicResultsPage;
import page.HomePage;
import page.ResultsVariant;

import java.time.Duration;

public class CriticalPathTest extends BaseTest {

    /**
     * Case 3: Kritik yol (arama -> uçuş seçimi -> rezervasyon sayfası)
     *
     * Bir kullanıcının bileti almaya giderken izlediği ana yolu uçtan uca
     * doğrular:
     *   1. Gidiş-dönüş araması yapılır
     *   2. İlk gidiş uçuşu ve paketi seçilir
     *   3. İlk dönüş uçuşu ve paketi seçilir
     *   4. Rezervasyon sayfasına ulaşıldığı doğrulanır
     *   5. İletişim ve yolcu formları doldurulup değerlerin forma işlendiği
     *      doğrulanır
     *   6. "Ödemeye ilerle" butonunun kullanılabilir olduğu doğrulanır
     *
     * SINIR: Test "Ödemeye ilerle" butonuna BASMAZ. Amaç kritik yolun
     * çalıştığını doğrulamak; gerçek bir satın alma akışı başlatmak değil.
     * Kullanılan tüm yolcu bilgileri açıkça sahte test verisidir.
     */
    @Test
    @Parameters({"originCity", "destinationCity", "departureDate", "returnDate"})
    public void testCriticalPathToReservationPage(
            @Optional("İstanbul") String originCity,
            @Optional("Ankara") String destinationCity,
            @Optional("2026-09-20") String departureDate,
            @Optional("2026-09-25") String returnDate) {

        HomePage homePage = new HomePage(getDriver());

        homePage.goToEnuygun()
                .acceptCookies()
                .selectRoundTrip()
                .selectOrigin(originCity)
                .selectDestination(destinationCity)
                .selectDates(departureDate, returnDate)
                .clickSearchButton();

        // Rezervasyon akışı (uçuş seçimi -> paket -> rezervasyon sayfası) yalnızca
        // KLASİK tasarım için implemente edildi ve doğrulandı. Site bir A/B testi
        // yürüttüğü için ("fly_search_facelift") diğer tasarım da gelebiliyor;
        // o durumda çerezleri temizleyip kovanın yeniden atanmasını deniyoruz.
        ResultsVariant variant;
        try {
            ResultsVariant.pinTo(getDriver(), ResultsVariant.CLASSIC, 4, Duration.ofSeconds(40));
            variant = ResultsVariant.CLASSIC;
        } catch (IllegalStateException e) {
            throw new SkipException(
                    "Kritik yol testi klasik sonuç sayfası tasarımı için yazıldı, ancak site " +
                    "A/B testi nedeniyle bu koşuda diğer tasarımı döndürdü ve kovaya yeniden " +
                    "atanamadı. Ayrıntı: " + e.getMessage());
        }
        Assert.assertEquals(variant, ResultsVariant.CLASSIC, "Beklenen tasarım elde edilemedi.");

        ClassicResultsPage resultsPage = new ClassicResultsPage(getDriver());
        CheckoutPage checkoutPage = resultsPage.selectFirstDepartureAndReturnFlight();

        // 1) Rezervasyon sayfasına ulaşıldı mı?
        Assert.assertTrue(checkoutPage.isCheckoutPageLoaded(),
                "Uçuş seçimi sonrası rezervasyon sayfasına ulaşılamadı. " +
                "Güncel URL: " + getDriver().getCurrentUrl());

        // 2) Formlar görünür mü?
        Assert.assertTrue(checkoutPage.isContactInfoSectionDisplayed(),
                "Rezervasyon sayfasında iletişim bilgileri formu görünmüyor.");
        Assert.assertTrue(checkoutPage.isPassengerInfoSectionDisplayed(),
                "Rezervasyon sayfasında yolcu bilgileri formu görünmüyor.");

        // 3) Formu AÇIKÇA SAHTE test verisiyle doldur
        String testEmail = "qa.automation.test@example.com";
        String testFirstName = "Test";
        String testLastName = "Kullanici";

        checkoutPage.fillContactInfo(testEmail, "5551112233")
                    .fillPassengerInfo(testFirstName, testLastName, "1", "1", "1990", true);

        // 4) Girilen değerler forma gerçekten işlendi mi?
        Assert.assertEquals(checkoutPage.getFirstNameValue(), testFirstName,
                "Yolcu adı forma yazılamadı.");
        Assert.assertEquals(checkoutPage.getLastNameValue(), testLastName,
                "Yolcu soyadı forma yazılamadı.");
        Assert.assertEquals(checkoutPage.getContactEmailValue(), testEmail,
                "İletişim e-postası forma yazılamadı.");

        // 5) Ödeme adımına geçiş mümkün mü? (Butona BASILMIYOR - bkz. sınıf yorumu)
        Assert.assertTrue(checkoutPage.isProceedPaymentButtonDisplayed(),
                "'Ödemeye ilerle' butonu bulunamadı; kritik yol ödeme adımına kadar tamamlanamıyor.");
    }
}
