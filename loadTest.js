import http from 'k6/http';
import { sleep, check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Ödev gereksinimi: siteye gerçek yük bindirmemek için 1 sanal kullanıcı yeterli.
// Amaç kapasite ölçmek değil; arama modülünün yanıt süresini ve hata oranını
// kontrollü biçimde gözlemlemek.
export const options = {
    vus: 1,
    duration: '5s',

    // Eşikler (thresholds): testin "geçti/kaldı" kriterleri. Bunlar olmadan
    // yük testi sadece sayı üretir, bir şey doğrulamaz.
    thresholds: {
        http_req_failed: ['rate<0.01'],        // hata oranı %1'in altında olmalı
        http_req_duration: ['p(95)<2000'],     // isteklerin %95'i 2sn altında
    },
};

const searchPageErrors = new Rate('search_page_errors');
const searchPageDuration = new Trend('search_page_duration');

const URL = 'https://www.enuygun.com/ucak-bileti/istanbul-ankara-esenboga-havalimani-ista-esb/';

export default function () {
    const res = http.get(URL, {
        headers: {
            // Gerçekçi bir istemci gibi davran: gerçek kullanıcı trafiğini
            // taklit etmeyen bir yük testi, ölçtüğü sayılar bakımından
            // yanıltıcı olur (CDN/WAF farklı davranabilir).
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) ' +
                          'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'tr-TR,tr;q=0.9',
        },
    });

    const ok = check(res, {
        'HTTP 200 döndü mü?': (r) => r.status === 200,
        'Gerçek sonuç sayfası mı (>100KB)?': (r) => r.body && r.body.length > 100000,
    });

    searchPageErrors.add(!ok);
    searchPageDuration.add(res.timings.duration);

    // TEŞHİS: başarısızlıkta ne olduğunu görmek için durum kodunu ve gövdenin
    // başını yazdır. (Sorun çözülünce bu blok kaldırılabilir.)
    if (!ok) {
        console.error(
            `BASARISIZ -> status=${res.status} ` +
            `boyut=${res.body ? res.body.length : 0}B ` +
            `son_url=${res.url}`
        );
        if (res.body) {
            console.error(`govde_basi: ${res.body.substring(0, 300).replace(/\s+/g, ' ')}`);
        }
    }

    sleep(1);
}
