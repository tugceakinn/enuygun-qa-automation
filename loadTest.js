import http from 'k6/http';
import { sleep, check } from 'k6';

// 1. Ayarlar (Requirements kısmındaki "1 user is enough" kuralını burada uyguluyoruz)
export const options = {
    vus: 1,           // Sadece 1 sanal kullanıcı (Siteye yük binmeyecek)
    duration: '5s',   // Test sadece 5 saniye sürecek
};

// 2. Senaryo (Arama modülünü tetikleme)
export default function () {
    // İstanbul - Ankara arası örnek bir uçuş listeleme URL'i
    const url = 'https://www.enuygun.com/ucak-bileti/istanbul-ankara-esenboga-havalimani-ista-esb/';

    // Siteye GET isteği atıyoruz
    const res = http.get(url);

    // Dönen cevabın başarılı (200 OK) olup olmadığını kontrol ediyoruz (Error Rate için gerekli)
    check(res, {
        'Sayfa basariyla yuklendi mi (Status 200)?': (r) => r.status === 200,
    });

    // Sistem arka arkaya spam atmasın diye 1 saniye mola
    sleep(1);
}