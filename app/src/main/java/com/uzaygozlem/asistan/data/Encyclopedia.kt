package com.uzaygozlem.asistan.data

/**
 * Gökcisimleri için gömülü (offline) kısa bilgi metinleri. Model/internet
 * gerektirmez; küçük bir dil modelinin uydurabileceği yanlışlardan uzak,
 * elle derlenmiş doğru bilgiler.
 */
object Encyclopedia {

    private val entries: Map<String, String> = mapOf(
        // --- Gezegenler ---
        "Merkür" to "Güneş'e en yakın gezegen. Hep alacakaranlıkta ufka yakın " +
            "göründüğü için yakalaması zordur; en iyi doğuş öncesi ya da batış " +
            "sonrası kısa pencerelerde bulunur.",
        "Venüs" to "Gökyüzünün Güneş ve Ay'dan sonra en parlak cismi. \"Akşam " +
            "yıldızı\" veya \"sabah yıldızı\" denir ama aslında gezegendir; kalın " +
            "bulut örtüsü Güneş ışığını çok iyi yansıtır.",
        "Mars" to "Kızıl Gezegen. Çıplak gözle turuncu-kırmızımsı sabit bir nokta " +
            "gibi görünür; yüzeyindeki demir oksit (pas) ona bu rengi verir.",
        "Jüpiter" to "Güneş Sistemi'nin en büyük gezegeni. Çok parlaktır; küçük bir " +
            "dürbünle bile yanındaki dört büyük uydusu (Galileo uyduları) noktalar " +
            "halinde seçilebilir.",
        "Satürn" to "Halkalı gezegen. Çıplak gözle sarımsı, sakin bir yıldız gibi " +
            "durur; küçük bir teleskopla ünlü halkaları ortaya çıkar.",

        // --- Ay ---
        "Ay" to "Dünya'nın tek doğal uydusu. Evreleri, Güneş'e göre konumundan " +
            "doğar: Yeni Ay'da karanlık, Dolunay'da tam aydınlıktır. Parlak Ay, " +
            "sönük gökcisimlerini ve meteorları görmeyi zorlaştırır.",

        // --- Parlak yıldızlar ---
        "Sirius (Akyıldız)" to "Gece gökyüzünün en parlak yıldızı. Büyük Köpek " +
            "takımyıldızındadır ve bize yakın (~8,6 ışık yılı) olduğu için bu kadar " +
            "parlak görünür.",
        "Canopus (Süheyl)" to "Gökyüzünün ikinci en parlak yıldızı. Türkiye'nin " +
            "güneyinden ufka yakın görülür; eski denizciler ve kervanlar yön bulmak " +
            "için ondan yararlanırdı.",
        "Arktürus" to "Kuzey göğünün en parlak yıldızı. Turuncu bir dev yıldızdır; " +
            "Büyük Ayı'nın kuyruğunu takip ederek kolayca bulunur.",
        "Vega" to "Yaz Üçgeni'nin en parlak köşesi. Dünya'nın ekseni kaydığı için " +
            "yaklaşık 12.000 yıl sonra yeni Kutup Yıldızı olacak.",
        "Capella" to "Arabacı takımyıldızının parlak sarı yıldızı. Aslında birbirine " +
            "yakın dört yıldızdan oluşan bir sistemdir.",
        "Rigel" to "Avcı (Orion) takımyıldızının mavi-beyaz süperdevi; avcının sol " +
            "ayağını temsil eder. Güneş'ten binlerce kat daha parlaktır.",
        "Procyon" to "Küçük Köpek takımyıldızının en parlak yıldızı ve bize en yakın " +
            "yıldızlardan biri.",
        "Betelgeuse" to "Avcı'nın omzundaki kırmızı süperdev. Öyle büyüktür ki " +
            "Güneş'in yerine koysak Mars'ın ötesine ulaşır; ömrünün sonuna yakın " +
            "olup günün birinde süpernova olarak patlayacak.",
        "Altair" to "Yaz Üçgeni'nin bir köşesi. Çok hızlı döndüğü için küreden çok " +
            "basık bir top şeklindedir.",
        "Aldebaran" to "Boğa takımyıldızının kırmızı gözü. Arkasındaki Hyades yıldız " +
            "kümesinin önünde durur ama ondan çok daha yakındır.",
        "Antares" to "Akrep takımyıldızının kalbi. Kırmızı bir süperdevdir; adı " +
            "Yunancada \"Mars'ın rakibi\" demektir, çünkü rengi Mars'a benzer.",
        "Spica (Başak)" to "Başak takımyıldızının en parlak yıldızı. Birbirine çok " +
            "yakın iki sıcak mavi yıldızdan oluşur.",
        "Pollux" to "İkizler takımyıldızının turuncu dev başı. Yanındaki Castor ile " +
            "birlikte ikizleri oluşturur; çevresinde bir gezegeni bulunur.",
        "Castor" to "İkizler'in diğer başı. Tek görünse de aslında altı yıldızdan " +
            "oluşan karmaşık bir sistemdir.",
        "Fomalhaut" to "Sonbahar göğünün yalnız parlak yıldızı. Çevresinde toz ve " +
            "kalıntı diski bulunan genç bir yıldızdır.",
        "Deneb" to "Yaz Üçgeni'nin en uzak köşesi (~2600 ışık yılı). Bu kadar uzaktan " +
            "bile parlak görünmesi, gerçekte devasa bir yıldız olmasındandır.",
        "Regulus" to "Aslan takımyıldızının kalbi. Ekliptiğe çok yakın olduğu için " +
            "zaman zaman Ay ve gezegenler önünden geçer.",
        "Kutup Yıldızı" to "Kuzeyi gösteren yıldız. Dünya'nın ekseni neredeyse tam " +
            "ona baktığı için gökyüzünde sabit durur; yüksekliği bulunduğun enleme " +
            "eşittir.",
        "Shaula" to "Akrep takımyıldızının iğnesindeki parlak yıldız.",
        "Algol" to "\"Şeytan Yıldızı.\" Her yaklaşık 3 günde bir, önünden geçen eş " +
            "yıldızı yüzünden çıplak gözle görülebilecek şekilde sönükleşir; " +
            "bilinen ilk değişen yıldızlardan.",
        "Mizar" to "Büyük Ayı'nın kuyruğundaki yıldız. Yanındaki Alcor ile birlikte " +
            "çıplak gözle çift görülür; eskiden göz keskinliği testi olarak kullanılırdı.",
        "Albireo" to "Kuğu takımyıldızının başı. Küçük bir dürbünle bakınca mavi ve " +
            "altın renkli, gökyüzünün en güzel çift yıldızlarından biri olarak görünür.",
        "Hamal" to "Koç takımyıldızının en parlak yıldızı.",
        "Alpheratz" to "Pegasus Karesi'nin bir köşesi; aynı zamanda Andromeda " +
            "takımyıldızına bağlıdır.",
        "Denebola" to "Aslan takımyıldızının kuyruğundaki yıldız.",
        "Alphecca (Gemma)" to "Kuzey Tacı takımyıldızının en parlak yıldızı; adı " +
            "\"mücevher\" anlamına gelir.",
    )

    private const val STAR_FALLBACK = "Çıplak gözle görülebilen parlak yıldızlardan " +
        "biri. Karanlık bir gökyüzünde takımyıldızları arasında yerini bulabilirsin."

    private val satelliteEntries: Map<String, String> = mapOf(
        "ISS" to "Uluslararası Uzay İstasyonu. İnsanlı en büyük yapı; ~400 km " +
            "yükseklikte, saatte ~28.000 km hızla 90 dakikada bir Dünya turu atar. " +
            "Güneş panellerinden yansıyan ışıkla çıplak gözle çok parlak görünür.",
        "Tiangong" to "Çin'in uzay istasyonu. Sürekli mürettebat barındırır; " +
            "ISS'ten küçük ama yine de çıplak gözle görülebilecek kadar parlaktır.",
        "Hubble Uzay Teleskobu" to "1990'dan beri Dünya yörüngesinde çalışan ünlü " +
            "uzay teleskobu. Alçak yörüngede olduğu için uygun geçişlerde gökyüzünde " +
            "hareket eden bir nokta olarak görülebilir.",
    )

    private val showerEntries: Map<String, String> = mapOf(
        "Quadrantidler" to "Yılın ilk büyük yağmuru. Zirvesi çok keskindir; yoğunluk " +
            "yalnızca birkaç saat sürdüğü için zamanlama önemlidir.",
        "Lyridler" to "İlkbaharın habercisi, orta yoğunlukta bir yağmur. Zaman zaman " +
            "beklenmedik patlamalar yapabilir.",
        "Eta Aquaridler" to "Halley kuyruklu yıldızının bıraktığı tozdan oluşur. " +
            "Hızlı meteorlarıyla bilinir; sabaha karşı en iyi izlenir.",
        "Perseidler" to "Yılın en popüler meteor yağmuru. Sıcak yaz gecelerinde " +
            "izlemesi kolaydır; hızlı ve parlak meteorlar, sık sık iz bırakır.",
        "Orionidler" to "Yine Halley'in tozundan doğar. Hızlı ve çoğu zaman iz " +
            "bırakan meteorlar üretir.",
        "Leonidler" to "Yaklaşık 33 yılda bir muhteşem \"meteor fırtınası\" yapmasıyla " +
            "ünlüdür; normal yıllarda ise sakin bir yağmurdur.",
        "Geminidler" to "Yılın en yoğun ve en güvenilir yağmuru. Yavaş, parlak ve " +
            "renkli meteorlar üretir; ilginç biçimde kaynağı bir kuyruklu yıldız " +
            "değil, bir asteroittir.",
        "Ursidler" to "Yılın son yağmuru, kutba yakın radyantıyla kuzeyden izlenir. " +
            "Mütevazı ama yıl kapanışı için hoş bir gösteridir.",
    )

    /** Gökcismi (yıldız/gezegen/Ay) açıklaması. */
    fun forSkyObject(name: String, isStar: Boolean): String =
        entries[name] ?: if (isStar) STAR_FALLBACK else ""

    /** Uydu açıklaması (kısa ada göre; "(özel)" eklenenlerde boş döner). */
    fun forSatellite(shortName: String): String = satelliteEntries[shortName] ?: ""

    fun forShower(name: String): String = showerEntries[name] ?: ""
}
