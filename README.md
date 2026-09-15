# ✈️ Enuygun QA Automation & Performance Testing Framework

Bu proje, **Enuygun.com** uçak bileti arama ve rezervasyon platformu için geliştirilmiş kapsamlı bir Uçtan Uca (E2E) Test Otomasyonu, API Testi, Yük Testi (Performance) ve Veri Kazıma (Data Scraping) çözümüdür.

---

## 📑 İçindekiler
- [Proje Genel Bakış ve Mimari](#-proje-genel-bakış-ve-mimari)
- [Kullanılan Teknolojiler ve Kütüphaneler](#-kullanılan-teknolojiler-ve-kütüphaneler)
- [Proje Klasör Yapısı](#-proje-klasör-yapısı)
- [Önemli Sınıflar ve Tasarım Desenleri (Design Patterns)](#-önemli-sınıflar-ve-tasarım-desenleri-design-patterns)
- [Test Kapsamı ve Senaryolar](#-test-kapsamı-ve-senaryolar)
  - [Part 1: UI Otomasyon Testleri](#part-1-ui-otomasyon-testleri)
  - [Part 2: API Testleri (REST Assured)](#part-2-api-testleri-rest-assured)
  - [Part 3: Yük & Performans Testi (k6)](#part-3-yük--performans-testi-k6)
  - [Part 4: Veri Çıkarımı & Analiz (Data Scraping)](#part-4-veri-çıkarımı--analiz-data-scraping)
- [Testlerin Çalıştırılması](#-testlerin-çalıştırılması)
- [Raporlama ve Hata Yakalama (Screenshots)](#-raporlama-ve-hata-yakalama-screenshots)

---

## 🏗 Proje Genel Bakış ve Mimari

Framework, endüstri standardı **Page Object Model (POM)** ve modern test otomasyonu pratikleri üzerine inşa edilmiştir:
- **Bakımı Kolay & Modüler:** Sayfa öğeleri (`locator`), sayfa aksiyonları (`page`) ve test mantığı (`tests`) birbirinden tamamen ayrılmıştır.
- **A/B Test Dayanıklılığı (Factory Pattern):** Enuygun'un canlı ortamda uyguladığı *fly_search_facelift* A/B testi nedeniyle gelen iki farklı sonuç sayfası tasarımı (Yeni Kart Tasarımı & Klasik Liste) `FlightResultsFactory` ve `FlightResults` arayüzü ile dinamik olarak yönetilir. Testler varyanttan bağımsız çalışır.
- **Cross-Browser & Thread-Safety:** `ThreadLocal<WebDriver>` kullanılarak Chrome ve Firefox üzerinde paralel veya sıralı koşumlar güvenli hale getirilmiştir.
- **Güçlü Senkronizasyon (Explicit Wait):** Kesinlikle `Thread.sleep` kullanılmamış; `WebDriverWait` ve `ExpectedConditions` ile stabil ve dinamik bekleme stratejileri uygulanmıştır.

---

## 🛠 Kullanılan Teknolojiler ve Kütüphaneler

| Kategori | Araç / Kütüphane | Kullanım Amacı |
| :--- | :--- | :--- |
| **Dil** | Java 21 | Temel programlama dili |
| **UI Otomasyon** | Selenium WebDriver (4.x) | Web tarayıcı etkileşimleri |
| **Test Runner & Assertions** | TestNG | Test yönetimi, parametrizasyon ve assertion'lar |
| **API Testing** | REST Assured | Petstore REST API CRUD & Negatif senaryolar |
| **Performance Testing** | k6 (Grafana) | Uçuş arama modülü HTTP yük & yanıt süresi testi |
| **Build & Dependency** | Apache Maven | Bağımlılık yönetimi ve test lifecycle kontrolü |
| **Raporlama & Utilities** | TestListener & ScreenshotUtils | Başarısız testlerde otomatik ekran görüntüsü ve HTML kaynak kaydı |

---

## 📂 Proje Klasör Yapısı

```
qa-interview-project/
├── screenshots/                   # Başarısız test ekran görüntüleri ve sayfa kaynakları (repoya dahil değil, .gitignore'da)
├── src/
│   └── test/
│       └── java/
│           ├── api/               # API Testleri
│           │   └── PetstoreApiTest.java
│           ├── base/              # Temel altyapı sınıfları
│           │   ├── BasePage.java
│           │   └── BaseTest.java
│           ├── model/             # Kazınan veri modeli
│           │   └── FlightRecord.java
│           ├── locator/           # Web Element By tanımları
│           │   ├── CheckoutPageLocator.java
│           │   ├── ClassicResultsPageLocator.java
│           │   ├── HomePageLocator.java
│           │   └── ResultsPageLocator.java
│           ├── page/              # Page Object ve Factory sınıfları
│           │   ├── CheckoutPage.java
│           │   ├── ClassicResultsPage.java
│           │   ├── FlightResults.java
│           │   ├── FlightResultsFactory.java
│           │   ├── PollingSupport.java
│           │   ├── HomePage.java
│           │   ├── ResultsPage.java
│           │   └── ResultsVariant.java
│           ├── tests/             # UI Test Senaryoları
│           │   ├── CriticalPathTest.java
│           │   ├── DataScraperTest.java
│           │   ├── FlightSearchTest.java
│           │   └── FlightSortTest.java
│           └── utils/             # Yardımcı araçlar ve dinleyiciler
│               ├── ScreenshotUtils.java
│               └── TestListener.java
├── flights.csv                    # Part 4 scraped uçuş verileri
├── loadTest.js                    # Part 3 k6 yük testi betiği
├── pom.xml                        # Maven bağımlılıkları ve yapılandırması
├── README.md                      # Proje dokümantasyonu
└── testng.xml                     # TestNG Suite & Cross-browser parametreleri
```

---

## 🧩 Önemli Sınıflar ve Tasarım Desenleri (Design Patterns)

### 1. `BaseTest` (Tarayıcı Yaşam Döngüsü ve Paralellik)
- `@BeforeMethod` ve `@AfterMethod` anotasyonları ile her test için bağımsız tarayıcı ayağa kaldırır ve kapatır.
- `ThreadLocal<WebDriver>` ile paralel testlerde session çakışmalarını önler.
- `testng.xml` üzerinden gelen `browser` parametresine göre **Chrome** veya **Firefox** başlatır (`Cross-Browser Testing`).
- `@Listeners(TestListener.class)` anotasyonu ile tüm testleri dinler.

### 2. `BasePage` (Ortak Sayfa Davranışları)
- `WebDriver` ve `WebDriverWait` nesnelerini kapsüller.
- `click()`, `writeText()`, `getText()`, `scrollToElement()`, `dragAndDropBy()` gibi ortak ve dayanıklı metotları barındırır.

### 3. A/B Test Çözümü: `FlightResultsFactory` & `FlightResults`
Enuygun, uçuş sonuç sayfasında iki farklı arayüz (Classic vs. Facelift) sunabilmektedir:
- `FlightResults` (Interface): Testlerin ihtiyaç duyduğu ortak operasyonları tanımlar (`applyDepartureTimeFilter`, `filterByAirlineOnly`, `sortByPriceAscending`, `getDisplayedPrices` vb.).
- `ResultsVariant`: Sayfa açıldığında DOM üzerindeki ayırt edici elementleri hızlıca tarayarak sayfa türünü tespit eder.
- `FlightResultsFactory`: Tespit edilen varyanta göre `ResultsPage` veya `ClassicResultsPage` nesnesini döner. Böylece test metotları hiçbir UI varyant detayına bağımlı kalmaz.

### 4. `TestListener` & `ScreenshotUtils`
- TestNG `ITestListener` arayüzünü uygular.
- Test başarısız olduğunda (`onTestFailure`), anlık ekran görüntüsünü (`.png`) ve sayfa DOM kaynağını (`.html`) `screenshots/` dizinine kaydeder.

---

## 🧪 Test Kapsamı ve Senaryolar

### Part 1: UI Otomasyon Testleri

#### Case 1: Temel Uçuş Arama ve Saat Filtreleme (`FlightSearchTest`)
- **Senaryo:** İstanbul -> Ankara gidiş-dönüş uçuş araması yapılır.
- **Filtre:** Kalkış saati filtresi **10:00 - 18:00** olarak ayarlanır (Slider veya saat aralığı butonları).
- **Doğrulamalar:**
  1. Sonuç listesinde uçuşların başarıyla listelendiği doğrulanır.
  2. Listelenen tüm uçuşların kalkış saatlerinin 10:00 ile 18:00 arasında olduğu doğrulanır.
  3. Rota doğrulaması: İstanbul'un tüm havalimanları (IST, SAW) ve Ankara (ESB) varış kodları kontrol edilir.

#### Case 2: Türk Hava Yolları Fiyat Sıralaması (`FlightSortTest`)
- **Senaryo:** İstanbul -> Ankara araması yapılır.
- **Filtre & Sıralama:** Yalnızca "Türk Hava Yolları" seçilir ve fiyata göre artan sırada sıralanır.
- **Doğrulamalar:**
  1. Filtre sonrası listenin boş olmadığı doğrulanır.
  2. Listelenen tüm uçuşların sadece "Türk Hava Yolları" olduğu teyit edilir.
  3. Fiyatların küçükten büyüğe (artan sırada) sıralandığı matematiksel olarak doğrulanır.

#### Case 3: Kritik Yol Testi (`CriticalPathTest`)
- **Senaryo:** Arama → gidiş uçuşu + paket seçimi → dönüş uçuşu + paket seçimi → rezervasyon sayfası.
- **Doğrulamalar:**
  1. Rezervasyon sayfasına (`/rezervasyon/detay`) ulaşıldığı doğrulanır.
  2. İletişim ve yolcu bilgileri formlarının görüntülendiği doğrulanır.
  3. Formlar sahte test verisiyle doldurulur ve değerlerin forma işlendiği doğrulanır.
  4. "Ödemeye ilerle" butonunun kullanılabilir olduğu doğrulanır.
- **Bilinçli sınır:** Test "Ödemeye ilerle" butonuna **basmaz**; gerçek bir satın alma akışı başlatılmaz. Kullanılan yolcu bilgileri açıkça sahte test verisidir, TC Kimlik alanı doldurulmaz.
- **Bilinen kısıt:** Rezervasyon akışı klasik sonuç sayfası tasarımı için implemente edildi ve doğrulandı. A/B testi nedeniyle diğer tasarım gelirse test, çerezleri temizleyip kovaya yeniden atanmayı dener; başarısız olursa açıklayıcı bir mesajla `SkipException` fırlatır.

---

### Part 2: API Testleri (REST Assured) (`PetstoreApiTest`)
Swagger Petstore v2 API üzerinde tam CRUD ve Negatif test döngüsü:
- **POST `/pet` (Create):** Yeni evcil hayvan kaydı oluşturma ve 200 OK ile body doğrulaması.
- **GET `/pet/{id}` (Read):** Oluşturulan kaydın ID ve isim alanlarının doğrulanması.
- **PUT `/pet` (Update):** Hayvanın ismi ve durumunun güncellenmesi ve teyidi.
- **DELETE `/pet/{id}` (Delete):** Kaydın silinmesi ve silinme onayının doğrulanması.
- **GET `/pet/{id}` (Negative Scenario):** Silinmiş kayda istek atıldığında `404 Not Found` ve `Pet not found` hata mesajı döndüğünün doğrulanması.

---

### Part 3: Yük & Performans Testi (k6) (`loadTest.js`)
- **Amaç:** Enuygun uçuş arama modülünün yanıt sürelerini ve hata oranını ölçmek.
- **Yapılandırma:** 1 Sanal Kullanıcı (VU) ile 5 saniyelik kontrollü yük.
- **Metrikler:** Status 200 kontrolü, HTTP response time (`http_req_duration`), hata oranı (`http_req_failed`).

---

### Part 4: Veri Çıkarımı & Analiz (`DataScraperTest`)
- İstanbul -> Lefkoşa rotası için gidiş-dönüş araması yapılır (rota ve tarihler parametrik).
- Her uçuştan **Havayolu, Kalkış, Varış, Süre, Aktarma Durumu ve Fiyat** bilgileri kazınır ve `flights.csv` dosyasına yazılır.
- **Kazıma page object katmanındadır** (`getFlightRecords`). Böylece A/B testinin döndürdüğü iki tasarım için de aynı alanlar üretilir ve CSV çıktısı varyanta göre değişmez.
- Yeni tasarımda bir uçuşun alanları tek bir kapsayıcıda toplanmadığı için alanlar, `data-testid`'lerin sonundaki **uçuş kimliğiyle** eşlenir — DOM sırasına güvenilmez.
- **Doğrulamalar:** kazımanın veri ürettiği, CSV satır sayısının kazınan uçuş sayısıyla eşleştiği ve tüm fiyatların pozitif olduğu kontrol edilir.
- CSV alanları kaçışlanır (virgül/tırnak içeren havayolu adları sütunları kaydırmasın).

---

## 🚀 Testlerin Çalıştırılması

### 1. UI Testlerini Çalıştırma (Maven & TestNG)

Tüm TestNG suite'ini (Chrome ve Firefox cross-browser testleri dahil) çalıştırmak için:
```bash
mvn clean test
```

Yalnızca belirli bir test sınıfını çalıştırmak için:
```bash
mvn test -Dtest=FlightSearchTest
mvn test -Dtest=FlightSortTest
```

TestNG XML dosyasını doğrudan belirtmek için:
```bash
mvn test -DsuiteXmlFile=testng.xml
```

### 2. API Testlerini Çalıştırma
```bash
mvn test -Dtest=PetstoreApiTest
```

### 3. k6 Yük Testini Çalıştırma
k6 yüklü ise:
```bash
k6 run loadTest.js
```

---

## 📊 Raporlama ve Hata Yakalama (Screenshots)

- Test koşumu sırasında bir hata meydana geldiğinde `TestListener` otomatik olarak devreye girer.
- Ekran görüntüsü ve sayfa kaynak kodu `screenshots/FAILURE_<TestMetotAdi>_<ZamanDamgasi>.png` formatında kaydedilir.
- TestNG varsayılan HTML raporlarına `target/surefire-reports/index.html` veya `target/surefire-reports/emailable-report.html` üzerinden erişilebilir.
