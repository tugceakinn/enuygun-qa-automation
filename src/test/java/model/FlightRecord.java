package model;

/**
 * Sonuç sayfasından kazınan tek bir uçuş satırı.
 *
 * Fiyat sayısal tutuluyor: iki tasarım da fiyatı data-price attribute'unda
 * ham sayı olarak veriyor, dolayısıyla "1.486 TL" gibi yerelleştirilmiş metni
 * parse etmeye gerek yok.
 */
public record FlightRecord(
        String airline,
        String departureTime,
        String arrivalTime,
        String duration,
        String connection,
        double price) {
}
