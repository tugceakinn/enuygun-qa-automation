package page;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.function.Predicate;

/**
 * Filtre/sıralama sonrası "liste güncellendi mi?" gibi koşulları beklerken
 * kullanılan ortak yardımcı.
 *
 * NEDEN GEREKLİ: Bu koşullar, sayfadaki elementleri OKUYARAK (getText,
 * getDomAttribute vb.) kontrol ediliyor. Ama React tarafı listeyi TAM O SIRADA
 * asenkron olarak yeniden render edebiliyor; bu durumda okuma yarıda kalan bir
 * element aniden DOM'dan kopar ve StaleElementReferenceException fırlatılır.
 *
 * Bu, WebDriverWait'in normalde kendiliğinden tolere ettiği bir durum DEĞİL:
 * FluentWait sadece koşul false dönerse tekrar dener; bir exception fırlarsa
 * (StaleElementReferenceException dahil) bekleme tamamen durur ve hata dışarı
 * yayılır. Bu yüzden StaleElementReferenceException'ı burada özellikle
 * yakalayıp "henüz hazır değil, tekrar dene" anlamına gelen false'a çeviriyoruz.
 */
final class PollingSupport {

    private PollingSupport() {
    }

    /**
     * Verilen koşulu WebDriverWait ile bekler; koşul StaleElementReferenceException
     * fırlatırsa bunu "henüz hazır değil" (false) olarak yorumlayıp beklemeye devam eder.
     * Süre dolarsa (TimeoutException) sessizce döner - asıl doğrulamayı çağıran
     * testteki assertion yapsın diye, boş bir teknik hata yerine anlamlı bir
     * "hangi veriler beklenenle uyuşmuyor" mesajı alınsın.
     */
    static void waitQuietly(WebDriverWait wait, Predicate<WebDriver> condition) {
        try {
            wait.until(d -> {
                try {
                    return condition.test(d);
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });
        } catch (TimeoutException ignored) {
            // Kasıtlı: bkz. sınıf yorumu.
        }
    }
}
