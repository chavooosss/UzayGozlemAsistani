# Uzay Gözlem Asistanı 🛰🌙

Gece gökyüzü gözlemi planlamak için yerel Android uygulaması: konumunuza göre
uydu geçişleri (ISS, Hubble, Tiangong), yaklaşan meteor yağmurları ve Ay evresi.

## Özellikler

- **Uydu Geçişleri** — Önümüzdeki 24 saat için her geçişin yükseliş/zirve/batış
  saati, pusula yönü ve en yüksek açısı. Görünürlük gerçekçi hesaplanır:
  uydu Dünya'nın gölgesindeyse veya gökyüzü henüz kararmadıysa (güneş > -6°)
  geçiş "görünmez" olarak işaretlenir ve sebebi yazılır. Liste aşağı çekilerek
  yenilenir; karta dokununca geçişin gökyüzündeki yolunu gösteren polar
  harita ve canlı pusula (detay ekranı) açılır. Pusula, cihazın yön
  sensörüyle uydunun yükseliş yönünü ok olarak gösterir; doğru yöne
  dönünce yeşile döner. (Not: manyetik kuzey kullanılır; Türkiye'de
  gerçek kuzeyden sapma ~5°, çıplak göz gözlemi için önemsiz.)
- **Bulutluluk** — Her geçişin zirve saatindeki bulut örtüsü yüzdesi
  (Open-Meteo, ücretsiz/anahtarsız; 1 saatlik önbellek). Yeşil ≤%30,
  altın ≤%70, kırmızı üstü. Tahmin alınamazsa çip gizlenir, uygulama
  etkilenmez.
- **Gökyüzü** (sekme) — O an ufkun üstündeki gezegenler (Merkür–Satürn),
  parlak yıldızlar (~18) ve Ay; yön/yükseklik/parlaklıkla listelenir ve
  polar gökyüzü haritasında gösterilir. Gezegen konumları Schlyter yörünge
  yöntemiyle hesaplanır (~1-2 açı dakikası). Gündüz uyarısı verir.
  Bir cisme dokununca **detay ekranı** açılır: anlık konum, doğuş/tepe/batış
  saatleri, karanlık görüş penceresi, telefonu doğrultarak bulma radarı ve
  gökyüzü haritası — yani uydulardaki her şeyin gezegen/yıldız/Ay eşdeğeri.
- **Akıllı gece planı** (Plan sekmesi, açılış ekranı) — Hesaplanan tüm veriden
  (görünür geçişler, parlak gezegenler, Ay, bulut, yaklaşan meteor yağmuru)
  kural tabanlı Türkçe bir öneri + öne çıkanlar listesi üretir. Model/internet
  gerektirmez, offline ve hatasızdır.
- **Offline ansiklopedi** — Bir yıldıza/gezegene/uyduya/meteor yağmuruna
  tıklayınca detayda "Hakkında" kartı: gömülü, doğrulanmış kısa bilgi.
- **Meteor detayı** — Yağmura dokununca radyantın anlık yönü/yüksekliği,
  pusula rehberi, en iyi izleme, kaynak cisim, etkin dönem, Ay etkisi.
  (Bildirim: her yağmurun zirve gecesi 21:00'de hatırlatma zaten kurulu.)
- **Yön filtresi** — Gökyüzü sekmesinde Tümü/Kuzey/Doğu/Güney/Batı çipleriyle
  o an görünen cisimleri baktığın yöne göre süz.
- **İzleme listesi** (İzleme sekmesi) — Bir uydu geçişini ya da gökcismini
  detayından "⭐ İzleme listesine ekle" ile kaydet; sonra tıklayınca güncel
  konumu/ne zaman görüneceği/pusulasıyla yeniden açılır. Gördüklerin (Günlük)
  ile incelemek istediklerin (İzleme) ayrı tutulur.
- **NORAD ID ile uydu ekleme** — "Uydu seç" içinden herhangi bir uydunun
  NORAD numarasını girerek (n2yo.com/celestrak.org'dan) katalog dışı uydu
  eklenir; TLE'den adı otomatik çekilir, kalıcı saklanır.
- **Uydu kataloğu** — 8 seçilebilir uydu (ISS/Tiangong/Hubble varsayılan +
  Terra/Aqua/Envisat/Landsat 8/NOAA-20). "🛰 Uydu seç" ile aç/kapat;
  seçim kalıcı.
- **Canlı dünya haritası** (Harita sekmesi) — Seçili uyduların anlık yer
  izdüşümü (5 sn'de bir) + ilk uydunun ±45 dk yer izi + gündüz/gece gölgesi
  (subsolar noktadan) + Güneş konumu + "Siz" konum işareti,
  equirectangular harita üzerinde.
- **Gözlem günlüğü** (Günlük sekmesi) — Geçiş detayındaki "✔ Gözlemledim"
  ile kayıt + not; JSON dosyada saklanır, silinebilir.
- **Canlı Takip** — Geçiş detayından açılır: uydunun o anki konumu her
  saniye hesaplanır, telefonun doğrultulduğu yön (pusula + eğim) ile
  karşılaştırılıp radar ekranında yönlendirme yapılır; uyduyu yakalayınca
  yeşil kilitlenir. Geçiş öncesi geri sayım gösterir, ekran açık kalır.
- **Parlaklık tahmini** — Zirvedeki menzilden yaklaşık kadir değeri
  (faz açısı ihmal edilir, ~±0.5 kadir). Küçük değer = parlak; ISS -2
  civarı tipiktir.
- **Widget** — Ana ekrana eklenebilir; bu geceki ilk görünür geçişi
  gösterir. Veri, uygulama her yenilendiğinde tazelenir.
- **Gece modu** — ☾ düğmesi tüm arayüzü koyu kırmızıya çevirir (göz
  karanlık adaptasyonunu korur).
- **Takvime ekle / Meteor bildirimi** — Geçişi takvim etkinliği yap;
  yağmur zirvesi akşamı 21:00'de hatırlatma al.
- **İlk açılış rehberi** — Uygulamayı ilk açanda sekmeleri tanıtan tek
  seferlik tur.
- **Paylaşım** — Gece planını ve bir geçişi düzgün Türkçe metin olarak
  paylaş (WhatsApp/Telegram vb.).
- **Boot dayanıklılığı** — Bildirim alarmları prefs'e kaydedilir; telefon
  yeniden başlarsa BootReceiver ile otomatik yeniden kurulur (uygulamayı
  açmaya gerek kalmadan).
- **Geçiş Bildirimi** — Görünür geçişlerden 10 dk önce bildirim
  (AlarmManager exact alarm + BroadcastReceiver; her yenilemede yeniden
  planlanır, telefon yeniden başlarsa uygulamayı bir kez açmak yeterli).
- **Meteor Takvimi** — 8 büyük yıllık yağmurdan en yakın 3'ü, kalan gün sayısı
  ve zirve gecesindeki Ay aydınlanması (parlak Ay = kötü gözlem) ile.
- **Ay Durumu** — Güncel evre (8 evre), aydınlanma yüzdesi, sonraki Yeni Ay /
  Dolunay tahmini.
- Gece kullanımına uygun koyu, kırmızı vurgulu tema.

## Derleme

```bash
# Günlük kullanım için optimize sürüm (R8, debug anahtarıyla imzalı, ~1.6MB):
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk

# Geliştirme sürümü:
./gradlew assembleDebug
```

Not: commons-logging bağımlılığı Android'de (R8 sonrası) çöktüğü için hariç
tutuldu; `org.apache.commons.logging` paketindeki mini stub onun yerine geçer.

Gereksinimler: JDK 17, Android SDK (platform 35). SDK yolu `local.properties`
içindeki `sdk.dir` ile ayarlanır.

## Mimari (basit MVVM)

```
app/src/main/java/com/uzaygozlem/asistan/
├── MainActivity.kt        # Compose UI girişi, sekmeler, izin akışı
├── MainViewModel.kt       # Tek StateFlow<UiState>; veri akışını yönetir
├── data/
│   ├── Satellites.kt      # Takip edilen uydular (NORAD ID'leri)
│   ├── TleRepository.kt   # CelesTrak'tan TLE çekme + 6 saatlik dosya önbelleği
│   └── MeteorShowers.kt   # Statik yağmur takvimi + en yakın 3'ün hesabı
├── astro/
│   ├── PassCalculator.kt  # SGP4 geçiş hesabı + görünürlük analizi
│   ├── SunCalc.kt         # Güneş yükseklik açısı (alacakaranlık kontrolü)
│   └── MoonCalc.kt        # Ay evresi/aydınlanması (elongasyon yöntemi)
├── location/
│   └── LocationProvider.kt # FusedLocation; izin yoksa null → manuel giriş
└── ui/                    # Compose ekranları (Geçişler, Meteorlar, Ay, özet)

app/src/main/java/com/github/amsacode/predict4java/
└── SatPosEclipse.java     # Kütüphanenin protected eclipse alanına paket-içi erişim
```

## Nasıl çalışıyor?

1. **TLE verisi**: `TleRepository`, CelesTrak'tan
   (`gp.php?CATNR={id}&FORMAT=tle`) yörünge elemanlarını çeker. Sonuç
   `filesDir`'e yazılır; 6 saatten tazeyse ağa hiç çıkılmaz. Ağ hatasında
   bayat önbellek kullanılır (uyarıyla), o da yoksa nazik bir hata gösterilir.
2. **Geçiş hesabı**: [predict4java](https://github.com/davidmoten/predict4java)
   (SGP4) ile 24 saatlik geçiş listesi çıkarılır.
3. **Görünürlük**: Her geçiş 30 saniyede bir örneklenir. Bir anda hem uydu
   güneş ışığı alıyorsa (gölgede değil) hem de gözlemcide güneş ufkun
   -6°'den aşağısındaysa geçiş GÖRÜNÜR sayılır ve görünürlük aralığı yazılır.
4. **Ay/Meteor**: Konum ve internet gerektirmez; basit astronomik formüllerle
   (güneş-ay ekliptik boylam farkı) cihazda hesaplanır.
5. Tüm saatler cihazın saat dilimiyle gösterilir.

## Konum

Uygulama önce konum izni ister (FusedLocationProvider). İzin verilmez ya da
konum alınamazsa enlem/boylamı elle girebilirsiniz; manuel konum kalıcı olarak
saklanır.

## Kapsam dışı (ilk sürümde yok)

Bildirim/alarm, çoklu dil, hesap sistemi, ışık kirliliği haritası.
