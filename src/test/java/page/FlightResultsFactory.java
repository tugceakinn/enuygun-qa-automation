package page;

import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * Sonuç sayfasının hangi tasarımda geldiğini tespit edip uygun
 * {@link FlightResults} implementasyonunu döner.
 */
public final class FlightResultsFactory {

    private FlightResultsFactory() {
    }

    public static FlightResults create(WebDriver driver, Duration timeout) {
        ResultsVariant variant = ResultsVariant.detect(driver, timeout);
        System.out.println("[A/B] Sonuç sayfası tasarımı: " + variant);

        return variant == ResultsVariant.FACELIFT
                ? new ResultsPage(driver)
                : new ClassicResultsPage(driver);
    }
}
