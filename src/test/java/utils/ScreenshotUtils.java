package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = "screenshots/";

    /**
     * Verilen WebDriver nesnesi ve isim ile ekran görüntüsü alır ve 'screenshots/' dizinine kaydeder.
     *
     * @param driver    Mevcut WebDriver nesnesi
     * @param imagePrefix Dosya adı ön eki (örn: test adı veya adım adı)
     * @return Kaydedilen dosyanın dosya yolu
     */
    public static String captureScreenshot(WebDriver driver, String imagePrefix) {
        if (driver == null) {
            System.err.println("Driver nesnesi null olduğu için ekran görüntüsü alınamadı.");
            return null;
        }

        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            File sourceFile = ts.getScreenshotAs(OutputType.FILE);

            Path dirPath = Paths.get(SCREENSHOT_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String sanitizedPrefix = imagePrefix.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String fileName = sanitizedPrefix + "_" + timestamp + ".png";
            Path destinationPath = dirPath.resolve(fileName);

            Files.copy(sourceFile.toPath(), destinationPath);
            System.out.println("[EKRAN GÖRÜNTÜSÜ KAYDEDİLDİ] -> " + destinationPath.toAbsolutePath());
            return destinationPath.toString();
        } catch (IOException e) {
            System.err.println("Ekran görüntüsü kaydedilirken hata oluştu: " + e.getMessage());
            return null;
        }
    }

    /**
     * Hata anında sayfanın tam HTML kaynağını 'screenshots/' dizinine kaydeder.
     * Site farklı ortamlarda (ör. farklı Chrome sürümü / A-B testi) FARKLI DOM
     * yapıları render edebiliyor; ekran görüntüsü görsel olarak yardımcı olsa da
     * gerçek data-testid/selector'leri göremiyoruz. Bu dosya sayesinde hatanın
     * o anki gerçek DOM'unu (locator'ları) inceleyebiliyoruz.
     */
    public static String capturePageSource(WebDriver driver, String imagePrefix) {
        if (driver == null) {
            return null;
        }
        try {
            Path dirPath = Paths.get(SCREENSHOT_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String sanitizedPrefix = imagePrefix.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String fileName = sanitizedPrefix + "_" + timestamp + "_source.html";
            Path destinationPath = dirPath.resolve(fileName);

            Files.writeString(destinationPath, driver.getPageSource());
            System.out.println("[SAYFA KAYNAĞI KAYDEDİLDİ] -> " + destinationPath.toAbsolutePath());
            return destinationPath.toString();
        } catch (IOException e) {
            System.err.println("Sayfa kaynağı kaydedilirken hata oluştu: " + e.getMessage());
            return null;
        }
    }
}
