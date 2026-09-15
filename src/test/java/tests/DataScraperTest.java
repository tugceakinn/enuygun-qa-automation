package tests;

import base.BaseTest;
import model.FlightRecord;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import page.FlightResults;
import page.FlightResultsFactory;
import page.HomePage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DataScraperTest extends BaseTest {

    private static final String CSV_FILE = "flights.csv";

    /**
     * Part 4: Veri çıkarımı ve analiz
     *
     * Sonuç sayfasındaki uçuşları kazıyıp CSV'ye yazar ve dosyanın gerçekten
     * doğru şekilde üretildiğini doğrular.
     *
     * NOT: Kazıma işi bilinçli olarak page object katmanında
     * ({@code getFlightRecords}). Böylece A/B testinin döndürdüğü iki farklı
     * tasarım için de aynı alanlar üretiliyor ve CSV çıktısı hangi varyantın
     * geldiğine göre değişmiyor. Test sadece "kazı, yaz, doğrula" akışını
     * yönetiyor.
     */
    @Test
    @Parameters({"scrapeOriginCity", "scrapeDestinationCity", "scrapeDepartureDate", "scrapeReturnDate"})
    public void scrapeFlightDataToCsv(
            @Optional("İstanbul") String originCity,
            @Optional("Lefkoşa") String destinationCity,
            @Optional("2026-10-15") String departureDate,
            @Optional("2026-10-20") String returnDate) throws IOException {

        HomePage homePage = new HomePage(getDriver());

        homePage.goToEnuygun()
                .acceptCookies()
                .selectRoundTrip()
                .selectOrigin(originCity)
                .selectDestination(destinationCity)
                .selectDates(departureDate, returnDate)
                .clickSearchButton();

        FlightResults results = FlightResultsFactory.create(getDriver(), Duration.ofSeconds(40));
        List<FlightRecord> flights = results.getFlightRecords();

        // 1) Kazıma gerçekten veri üretti mi?
        Assert.assertFalse(flights.isEmpty(),
                "Sonuç sayfasından hiç uçuş kazınamadı. Arama sonuçsuz kalmış veya " +
                "kazıma locator'ları değişmiş olabilir.");

        Path csvPath = Paths.get(CSV_FILE).toAbsolutePath();
        writeCsv(csvPath, flights);
        System.out.println("[CSV YAZILDI] " + flights.size() + " uçuş -> " + csvPath);

        // 2) Dosya diske gerçekten yazıldı mı ve satır sayısı tutuyor mu?
        Assert.assertTrue(Files.exists(csvPath), "CSV dosyası oluşturulamadı: " + csvPath);

        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        Assert.assertEquals(lines.size(), flights.size() + 1,
                "CSV satır sayısı kazınan uçuş sayısıyla uyuşmuyor (başlık satırı dahil).");

        // 3) Fiyatlar sayısal ve pozitif mi? (Veri kalitesi kontrolü)
        List<String> invalidPrices = new ArrayList<>();
        for (FlightRecord flight : flights) {
            if (flight.price() <= 0) {
                invalidPrices.add(flight.airline() + " " + flight.departureTime());
            }
        }
        Assert.assertTrue(invalidPrices.isEmpty(),
                "Fiyatı sıfır veya negatif olan uçuşlar var: " + invalidPrices);
    }

    private void writeCsv(Path path, List<FlightRecord> flights) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Airline,Departure,Arrival,Duration,Connection,Price\n");

        for (FlightRecord flight : flights) {
            csv.append(escape(flight.airline())).append(',')
               .append(escape(flight.departureTime())).append(',')
               .append(escape(flight.arrivalTime())).append(',')
               .append(escape(flight.duration())).append(',')
               .append(escape(flight.connection())).append(',')
               .append(flight.price()).append('\n');
        }

        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    /**
     * CSV alan kaçışlaması: havayolu adı veya aktarma bilgisi virgül ya da
     * tırnak içerebilir; kaçışlamazsak sütunlar kayar ve dosya bozulur.
     * (Eski sürüm alanları doğrudan birleştiriyordu.)
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
