package page;

import locator.ResultsPageLocator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * enuygun'un sonuç sayfası iki farklı tasarımda gelebiliyor.
 *
 * Site "fly_search_facelift" adlı bir A/B testi yürütüyor:
 *   B -> FACELIFT : yeni tasarım (card/timeline, data-testid tabanlı)
 *   A -> CLASSIC  : klasik tasarım (flight-item, rc-slider)
 *
 * Kova, oturum çerezine (weg_sid) göre atandığı ve Selenium her koşuda temiz
 * bir profil açtığı için hangi tasarımın geleceği KOŞUDAN KOŞUYA DEĞİŞİYOR.
 * Bu, testlerin zaman zaman "sebepsiz" TimeoutException ile patlamasının
 * gerçek nedeniydi.
 *
 * Gerçek bir projede doğru çözüm, geliştirici ekipten feature-flag'i zorlama
 * imkânı istemek (cookie/header/SDK override) ve her varyantı ayrı ayrı
 * deterministik olarak test etmektir. Bu harici bir site olduğu için o erişim
 * yok; elimizdeki tek zorlama yöntemi, oturum çerezini temizleyip kovanın
 * yeniden atanmasını sağlamak.
 */
public enum ResultsVariant {

    FACELIFT,
    CLASSIC;

    /**
     * Sayfada hangi tasarımın render edildiğini tespit eder.
     *
     * DİKKAT: "div.flight-item" HER İKİ tasarımda da bulunuyor, bu yüzden
     * ayırt edici olarak KULLANILAMAZ. Aşağıdaki işaretler tasarıma özgüdür.
     */
    public static ResultsVariant detect(WebDriver driver, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout).until(d -> {
                if (!d.findElements(ResultsPageLocator.FACELIFT_MARKER).isEmpty()) {
                    return FACELIFT;
                }
                if (!d.findElements(ResultsPageLocator.CLASSIC_MARKER).isEmpty()) {
                    return CLASSIC;
                }
                return null;
            });
        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Sonuç sayfasının hangi tasarımda geldiği tespit edilemedi. " +
                    "Ne '" + ResultsPageLocator.FACELIFT_MARKER + "' (yeni tasarım) " +
                    "ne de '" + ResultsPageLocator.CLASSIC_MARKER + "' (klasik tasarım) bulundu. " +
                    "Sayfa hiç yüklenmemiş, arama sonuçsuz kalmış veya site tasarımı " +
                    "yeniden değişmiş olabilir.", e);
        }
    }

    /**
     * İstenen tasarım gelene kadar oturum çerezlerini temizleyip sayfayı
     * yeniden yükler (A/B kovasını yeniden attırır).
     *
     * @return kaç denemede istenen varyanta ulaşıldığı
     */
    public static int pinTo(WebDriver driver, ResultsVariant desired, int maxAttempts, Duration timeout) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ResultsVariant current = detect(driver, timeout);
            if (current == desired) {
                System.out.println("[A/B] Hedef tasarım (" + desired + ") " + attempt + ". denemede elde edildi.");
                return attempt;
            }

            System.out.println("[A/B] " + attempt + ". deneme: " + current +
                    " geldi, hedef " + desired + ". Çerezler temizlenip yeniden denenecek.");

            driver.manage().deleteAllCookies();
            ((JavascriptExecutor) driver).executeScript("location.reload();");
        }

        throw new IllegalStateException(
                "A/B testi nedeniyle hedeflenen '" + desired + "' tasarımı " + maxAttempts +
                " denemede elde edilemedi. Site kova dağılımını değiştirmiş olabilir; " +
                "bu durumda diğer varyant için de implementasyon gerekir.");
    }
}
