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
- [Otomasyon Sırasında Bulunan Site Davranışları](#-otomasyon-sırasında-bulunan-site-davranışları)
- [Araç Seçimi Üzerine Not](#-araç-seçimi-üzerine-not)
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
- **Yapılandırma:** 1 Sanal Kullanıcı (VU), 5 saniye. Siteye gerçek yük bindirmemek için bilinçli olarak küçük tutuldu; amaç kapasite ölçmek değil, yanıt süresi ve hata oranını kontrollü biçimde gözlemlemek.

#### Eşikler (thresholds)
Yük testinin "geçti/kaldı" kriterleri. Eşik tanımlanmazsa k6 yalnızca sayı üretir, hiçbir şey doğrulamaz — assertion'ı olmayan bir test gibi.

```js
thresholds: {
    http_req_failed:   ['rate<0.01'],    // hata oranı %1'in altında
    http_req_duration: ['p(95)<2000'],   // isteklerin %95'i 2sn altında
}
```

#### Sonuçlar

| Metrik | Değer | Eşik | Sonuç |
| :--- | ---: | :--- | :---: |
| Toplam istek | 5 | — | — |
| Başarılı kontrol oranı | %100 | — | ✅ |
| Hata oranı (`http_req_failed`) | %0 | < %1 | ✅ |
| Ortalama yanıt süresi | 96.4 ms | — | — |
| Medyan | 85.4 ms | — | — |
| p(95) | 143.1 ms | < 2000 ms | ✅ |
| En yüksek | 157.0 ms | — | — |
| İlk bayta kadar (`http_req_waiting`) ort. | 43.2 ms | — | — |

Arama sayfası 1 kullanıcı altında sorunsuz yanıt veriyor; p(95) değeri eşiğin çok altında.

#### Bu bölümde çıkan bulgu
İlk koşuda **5 isteğin 5'i de başarısızdı** ve dönen yanıtlar yalnızca ~8 KB'tı (gerçek sayfa ~730 KB). Sebep: varsayılan k6 isteği gerçekçi istemci başlıkları göndermiyor ve site bu tür isteklere farklı yanıt veriyor.

Bu yüzden teste iki şey eklendi:
1. Gerçekçi `User-Agent`, `Accept` ve `Accept-Language` başlıkları — gerçek kullanıcı trafiğini taklit etmeyen bir yük testi yanıltıcı sayılar üretir.
2. Sadece "HTTP 200 mü" değil, **"gerçek sonuç sayfası mı (>100 KB)"** kontrolü — çünkü bir hata/challenge sayfası da 200 dönebilir. Tek başına durum kodu kontrolü bu hatayı gizlerdi.

---

### Part 4: Veri Çıkarımı & Analiz (`DataScraperTest`)
- İstanbul -> Lefkoşa rotası için gidiş-dönüş araması yapılır (rota ve tarihler parametrik).
- Her uçuştan **Havayolu, Kalkış, Varış, Süre, Aktarma Durumu ve Fiyat** bilgileri kazınır ve `flights.csv` dosyasına yazılır.
- **Kazıma page object katmanındadır** (`getFlightRecords`). Böylece A/B testinin döndürdüğü iki tasarım için de aynı alanlar üretilir ve CSV çıktısı varyanta göre değişmez.
- Yeni tasarımda bir uçuşun alanları tek bir kapsayıcıda toplanmadığı için alanlar, `data-testid`'lerin sonundaki **uçuş kimliğiyle** eşlenir — DOM sırasına güvenilmez.
- **Doğrulamalar:** kazımanın veri ürettiği, CSV satır sayısının kazınan uçuş sayısıyla eşleştiği ve tüm fiyatların pozitif olduğu kontrol edilir.
- CSV alanları kaçışlanır (virgül/tırnak içeren havayolu adları sütunları kaydırmasın).

---

## 🐞 Otomasyon Sırasında Bulunan Site Davranışları

Testleri yazarken karşılaşılan ve **otomasyonu doğrudan etkileyen** site davranışları. Her biri testlerde nasıl ele alındığıyla birlikte aşağıda; ilgili sınıflarda da javadoc olarak belgelendi.

| # | Bulgu | Etki | Testte ele alınışı |
| :-- | :--- | :--- | :--- |
| 1 | **Arama sonuçları sayfası A/B testinde** (`fly_search_facelift`). İki tasarım yapısal olarak farklı: biri `data-testid` tabanlı kart/timeline, diğeri `div.flight-item` + `rc-slider`. Varyant `weg_sid` oturum çerezine bağlı olduğu için her temiz profilde rastgele düşüyor. | Tek bir locator seti ile kararlı test yazmak mümkün değil. | Çalışma anında varyant tespiti (`ResultsVariant.detect`) + `FlightResults` arayüzü ve iki implementasyon (Strategy/Factory). Testler hangi tasarımın geldiğini bilmiyor. |
| 2 | **Yanıltıcı `data-testid`.** `flight-oneWayCheckbox-*` ön eki üç ayrı kutuda kullanılıyor (`checkbox-transitFilter`, `checkbox-showListHotel` dahil) ve hiçbiri tek yön kutusu değil. | `data-testid` ile seçim yapan her otomasyon yanlış elemente tıklar. | Bu ailedeki testid'ler kullanılmadı; kutular `input[id^='checkbox-showListHotel']` gibi kimlik tabanlı locator'larla seçildi. |
| 3 | **"Bu tarihler için otelleri de listele" kutusu varsayılan olarak işaretli.** | Arama, uçuş sonuçları yerine otel listeleme sayfasına gidiyor; test sessizce yanlış sayfada devam ediyor. | `HomePage.ensureHotelListingUnchecked()` aramadan önce kutuyu kontrol edip kaldırıyor. |
| 4 | **İletişim telefonu alanındaki `tr-mask-phone-number` maskesi**, alan boşken ilk tuşta başa kendi "5"ini ekliyor ve toplamı 10 haneye kırpıyor (HTML'de `maxlength` yok, sınır JS'te). `5551112233` yazıldığında alanda `555 511 1223` kalıyor. | WebDriver ile tuş tuş yazılan her değer bir hane kayıyor. | `CheckoutPage.typeDigitsRespectingMask()`: her haneden önce alanın gerçek değeri okunuyor, maskenin zaten koyduğu hane tekrar yazılmıyor. Alan testin assertion'larına dahil olmadığı için, yine de tutmazsa test düşürülmüyor — konsola görünür bir `[UYARI]` basılıyor. |
| 5 | **İletişim e-postası alanına jQuery UI autocomplete bağlı** (`ui-autocomplete-input`). Öneri kutusu açık kalırsa odak kaybında yazılan değeri geri alıyor — **Firefox'ta** boş kalıyor, Chrome'da sorun çıkmıyordu. | Aynı test tarayıcıya göre farklı sonuç veriyor. | Yazdıktan sonra `ESCAPE` ile öneri kutusu kapatılıyor, ardından değer DOM property'sinden okunup doğrulanıyor. |
| 6 | **Kalkış saati slider'ı, tek seferlik sürüklemede filtrelemiyor.** Input'un `value` değeri değişiyor ve ekranda da doğru görünüyor, ama liste filtrelenmiyor; site ardışık `mousemove` olayları bekliyor. | "Slider doğru değerde" diye doğrulayan bir test yeşil kalır ama hiçbir şeyi test etmemiş olur. | Sürükleme 10 adımlı ardışık hareketle yapılıyor (`ResultsPage.dragThumb`). Programatik değer atama (`value` setter + event dispatch) bilinçli olarak **kaldırıldı**: doğrulamayı geçiriyor ama gerçek hatayı gizliyordu. |
| 7 | **Doğrudan HTTP istekleri bot korumasına takılıyor.** Tarayıcı başlıkları olmadan yapılan isteklere ~8 KB'lık bir challenge sayfası, üstelik **HTTP 200** ile dönüyor. | Sadece `status === 200` kontrol eden bir yük testi, tek bir gerçek sayfa servis edilmese bile %100 başarılı görünür. | `loadTest.js` gerçekçi `User-Agent`/`Accept` başlıkları gönderiyor ve gövde boyutunu da kontrol ediyor; eşikler (`http_req_failed`, `p(95)`) ayrıca tanımlı. |

> Not: 4 ve 5 numaralı maddeler tarayıcıya göre farklı davrandığı için testler hem Chrome hem Firefox üzerinde çalıştırıldı; bu tür uyumsuzlukların ancak çapraz tarayıcı koşusunda görünür olduğu bu projede somut olarak gözlendi.

---

## 🔍 Araç Seçimi Üzerine Not

Ödevde "Python or Java + Selenium would be preferable" denildiği için **Java + Selenium** tercih edildi.

Proje boyunca karşılaşılan hataların bir kısmı siteye özgüydü (A/B testi, maskeli alanlar, yanıltıcı `data-testid`'ler) ve hangi araçla çalışılsa yaşanırdı. Ancak bir kısmı **doğrudan Selenium'un çalışma modelinden** kaynaklandı:

| Karşılaşılan sorun | Playwright'ta karşılığı |
| :--- | :--- |
| Liste yeniden render olunca element referansının kopması (`StaleElementReferenceException`) | Locator'lar tembeldir, her aksiyonda yeniden çözülür — bu hata sınıfı yoktur |
| `clear()` maskeli alanı boşaltamıyor | `fill()` alanı temizleyip yazar |
| Element hazır değilken yapılan tıklamalar | Yerleşik actionability kontrolü |
| Hata anını yeniden üretememe | Trace viewer / video yerleşik |

Gerçek bir projede bu maliyet ekiple konuşulmaya değer bir konudur. Bu ödevde belirtilen tercihe uyuldu; yaşanan sorunlar ve çözümleri yukarıdaki bölümlerde belgelendi.

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
