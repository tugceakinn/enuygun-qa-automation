package utils;

import base.BaseTest;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("==================================================");
        System.out.println("[TEST BAŞLADI] " + result.getName() + " - " + result.getMethod().getDescription());
        System.out.println("==================================================");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("[TEST BAŞARILI] " + result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.err.println("[TEST BAŞARISIZ OLDU] " + result.getName() + " -> " + result.getThrowable());

        WebDriver driver = BaseTest.getDriver();
        if (driver != null) {
            String savedPath = ScreenshotUtils.captureScreenshot(driver, "FAILURE_" + result.getName());
            if (savedPath != null) {
                System.out.println("[HATA EKRAN GÖRÜNTÜSÜ ALINDI] -> " + savedPath);
            }

            String sourcePath = ScreenshotUtils.capturePageSource(driver, "FAILURE_" + result.getName());
            if (sourcePath != null) {
                System.out.println("[HATA SAYFA KAYNAĞI ALINDI] -> " + sourcePath);
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("[TEST ATLANDI] " + result.getName());
    }

    @Override
    public void onStart(ITestContext context) {
        System.out.println("[SUITE BAŞLADI] " + context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("[SUITE TAMAMLANDI] " + context.getName());
    }
}
