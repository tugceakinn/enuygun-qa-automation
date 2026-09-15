package page;

import model.FlightRecord;

import java.time.LocalTime;
import java.util.List;

/**
 * Uçuş sonuç sayfasının, testlerin ihtiyaç duyduğu davranışları.
 *
 * enuygun bir A/B testi ("fly_search_facelift") yürüttüğü için sonuç sayfası
 * iki farklı tasarımda gelebiliyor ve ikisinin DOM'u tamamen farklı:
 *   - {@link ResultsPage}        : yeni tasarım (card/timeline)
 *   - {@link ClassicResultsPage} : klasik tasarım (flight-item / rc-slider)
 *
 * Testler bu arayüz üzerinden çalışır; hangi implementasyonun kullanılacağına
 * {@link FlightResultsFactory} çalışma anında karar verir. Böylece test kodu
 * hangi varyantın geldiğini bilmek zorunda kalmaz.
 */
public interface FlightResults {

    FlightResults applyDepartureTimeFilter(int fromHour, int toHour);

    FlightResults filterByAirlineOnly(String airlineName);

    FlightResults sortByPriceAscending();

    List<LocalTime> getDisplayedDepartureTimes();

    List<Double> getDisplayedPrices();

    List<String> getDisplayedAirlines();

    List<String> getDisplayedOriginCodes();

    List<String> getDisplayedDestinationCodes();

    int getDisplayedFlightCount();

    /**
     * Listelenen uçuşları yapılandırılmış kayıtlar olarak döner (Part 4 - veri
     * çıkarımı). Her iki tasarım da aynı alanları üretir, böylece CSV çıktısı
     * hangi varyantın geldiğine bağlı olarak değişmez.
     */
    List<FlightRecord> getFlightRecords();
}
