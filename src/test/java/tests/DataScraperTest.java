package tests;

import base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;
import page.HomePage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DataScraperTest extends BaseTest {

    @Test
    public void scrapeFlightData() {
        // getDriver() kullanarak BaseTest'teki WebDriver'a güvenli erişim sağlıyoruz
        HomePage homePage = new HomePage(getDriver());

        // 1. Arama yapma (İstanbul -> Lefkoşa)
        homePage.goToEnuygun()
                .acceptCookies()
                .selectOrigin("İstanbul")
                .selectDestination("Lefkoşa")
                .selectDates("2026-10-15", "2026-10-20")
                .clickSearchButton();

        // 2. Sayfanın ve uçuş listesinin yüklenmesini Explicit Wait (WebDriverWait) ile bekle
        org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(getDriver(), java.time.Duration.ofSeconds(15));
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(By.cssSelector(".flight-item, [data-testid='flight-item'], .flight-list-body")));

        // 3. Uçuş kartlarını yakalama (DİKKAT: .flight-item gibi class'ları inspect ile güncellemelisin)
        List<WebElement> flightCards = getDriver().findElements(By.cssSelector(".flight-item"));

        // 4. CSV dosyasını oluşturma ve veri yazma
        try (FileWriter writer = new FileWriter("flights.csv")) {
            // CSV Başlıkları
            writer.append("Airline,Departure_Arrival,Duration,Connection,Price\n");

            for (WebElement card : flightCards) {
                // Kartın içinden ilgili dataları çekiyoruz
                String airline = card.findElement(By.cssSelector(".airline-name")).getText();
                String time = card.findElement(By.cssSelector(".flight-time")).getText();
                String duration = card.findElement(By.cssSelector(".flight-duration")).getText();
                String connection = card.findElement(By.cssSelector(".connection-info")).getText();

                // Fiyatı string'den integer'a çevirmeye uygun hale getiriyoruz (Örn: "1.500 TL" -> "1500")
                String price = card.findElement(By.cssSelector(".price")).getText().replace(" TL", "").replace(".", "");

                // CSV'ye virgüllerle ayırarak satır olarak ekle
                writer.append(airline).append(",")
                        .append(time).append(",")
                        .append(duration).append(",")
                        .append(connection).append(",")
                        .append(price).append("\n");
            }
            System.out.println("Veriler başarıyla flights.csv dosyasına yazıldı!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}