# Credit Score Logic — What I Built

Bu dosya `CreditScoreService`'i nasil yazdigimizi anlatir. Amac: gercek bir Findeks gibi,
kullanicinin **haftalik degisen** bir kredi skoru olsun ve **gecmisi saklansin**.

## The bug we started with

Eski `calculateMockScore` hep **0** donduruyordu:

```java
return Math.abs((sum * 1900) % 1900);   // 1900'un her kati % 1900 = 0 -> HEP 0
```

`* 1900` yuzunden. 1900'un herhangi bir kati, 1900'e bolununce hep 0 kalan verir.
Cozum: 1900'e **asal** bir carpan kullan (ya da baska bir formul).

## Snapshot mi, delta mi? (Onemli karar)

Skoru nasil saklayalim iki yol var:

**Delta storage (banka defteri gibi)** — her hafta *degisimi* saklarsin (+40, -15...).
Bugunku skoru bulmak icin hepsini **SUM**'larsin. Deger hesaplanir.

**Snapshot storage (BİZİM SECTIGIMIZ)** — her hafta *mutlak skoru* saklarsin (1240, 1225...).
Bugunku skor = en son satiri **oku**. Toplama yok.

| week | delta yolu | snapshot yolu |
| -----|------------|---------------|
| 0    | +1200      | 1200          |
| 1    | +40        | 1240          |
| 2    | -15        | 1225          |

Ikisi de ayni gecmisi tutar; fark **hangi sayiyi yazdigin**.

### Neden snapshot?
- Bir skorda sorulan sey **mutlak deger** ("skorum kac"), degisim degil -> okuma hic hesap istemez.
- **Clamp (1-1900)** dogru kalir. Delta+SUM'da tavana vurunca toplam artmaya devam eder ama skor sabit -> ikisi ayrilir. Snapshot'ta o haftanin clamp'li degerini direkt yazarsin.
- Gercek kredi gecmisi zaten **tarihli mutlak skorlar listesi**.

**Not:** delta yine var ama sadece **bir sonrakini URETMEK** icin (kilo defteri gibi: her hafta guncel kiloyu yazarsin, farki cikararak bulursun).

### Para = SUM, skor = LATEST (genel ders)
- Para bir **birikimdir** -> guncel = deltalarin **SUM**'i.
- Skor bir **gozlemdir** -> guncel = en son **snapshot** (en yuksek `weekIndex`).
- Ikisi de append-only + sirali; sadece sondaki "fold" farkli.

## The model: mean-reverting random walk

Skor her hafta oncekinden **kayar**. Ilk hali saf random walk'ti ama o **duvara yapisiyordu**
(1900'e cikip orada kaliyordu). Gercek skorlar boyle olmaz.

Cozum: **yuksekken kazanmak zor, dusukken kaybetmek zor.** Bunun adi
**mean reversion** (Ornstein-Uhlenbeck / AR(1), finansta standart).

```java
newScore = clamp( current + REVERSION * (base - current) + noise )
//                          \___ base'e cekim ___/          \noise/
```

- `current` base'in **ustundeyse** -> `(base-current)` negatif -> asagi cekim -> kazanmak zor
- `current` base'in **altindaysa** -> pozitif cekim -> kaybetmek zor
- ne kadar uzaklasirsan cekim o kadar guclu -> 1900/1 **nadir ve gecici** olur (yine de mumkun)
- `clamp` artik sadece son guvenlik siniri, nadiren dokunulur

`REVERSION` bir "yapiskanlik" ayari: 0 = saf yuruyus, 1 = aninda base'e yapisir. 0.35 dogal duruyor.

## Everything derives from the ID (tcNo), deterministically

Gercek veri olsaydi bunlarin yeri gercek finansal veri olurdu; sekil ayni kalirdi.

- **base** (baslangic/merkez skor): tcNo'dan turer, **600..1400** bandinda
- **volatility** (haftalik oynaklik/noise genligi): tcNo'dan turer, **50..500** arasi (kisiye ozel)
- **weeklyDelta (noise)**: (tcNo, hafta)'dan turer, **-vol..+vol**
- hepsi deterministik: ayni girdi -> ayni cikti

## The hash bug we caught by ACTUALLY RUNNING it

Ilk denemede bir kullanicinin skoru cok **duz** cikti (+116, +76, +48... her adim ~0.65 kat kuculuyordu, rastgele degil rampa gibi).

Sebep: noise'u `Objects.hash(tcNo, "W"+week)` ile uretiyordum. Ardisik haftalarda
`"W2886"` ve `"W2887"` hashCode'lari ~1 fark ediyor -> noise haftadan haftaya ~+1 artiyor,
"rastgele yuruyus" **duz bir rampaya** donuyor. Kotu karistirma.

Cozum: **splitmix64-tarzi** bir mixer (carpim + xor-shift zinciri). Ardisik haftalari bile
birbirinden bagimsiz dagitir.

```java
private int seed(User user, long week, String salt) {
    long h = user.getTcNo().hashCode() * 0x9E3779B97F4A7C15L
           + week                      * 0xC2B2AE3D27D4EB4FL
           + salt.hashCode()           * 0x165667B19E3779F9L;
    h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL;
    h ^= (h >>> 33); h *= 0xc4ceb9fe1a85ec53L;
    h ^= (h >>> 33);
    return (int) h;
}
```

**Ders:** kodu calistirmadan "derleniyor = calisiyor" sanma. Test edince gercek deger dagilimi
hatayi gosterdi.

## How it's stored & generated

- Her hafta = bir **satir** (snapshot). Tablo: `credit_scores`, yeni kolon `week_index`.
- **weekIndex = epochDay / 7** (7 gunluk pencere). Ayni zamanda **sira**: en yuksek weekIndex = en guncel.
- `weekStart = LocalDate.ofEpochDay(weekIndex * 7)` -> DTO'da tarihi boyle turetiyoruz (queriedAt'ten degil, cunku toplu uretimde hepsi ~ayni ani gosterir).
- **Unique constraint `(user_id, week_index)`** -> ayni haftaya iki snapshot giremez. Es zamanli iki sorguda **DB son hakem** (register'daki unique email mantiginin aynisi).
- `queryScore` cagrilinca `generateMissingWeeks`: hesabin acildigi haftadan bu haftaya kadar eksik haftalari **uretip ekler** (append-only), sonra **en son snapshot'i** doner.
- `getHistory`: tum snapshot'lar eskiden yeniye.

### @Transactional
`queryScore` **@Transactional** — icindeki tum yazmalar (snapshot'lar + audit log) tek transaction,
**ya hepsi ya hicbiri (atomic)**. Burada is *atomiklik*, kilitleme degil (hepsi INSERT, paylasilan kolon yok).

## Repository (Spring Data JPA method isminden SQL uretir)

```java
Optional<CreditScore> findTopByUserOrderByWeekIndexDesc(User user); // en guncel snapshot ("latest = highest seq")
List<CreditScore>      findByUserOrderByWeekIndexAsc(User user);     // tum gecmis, zaman cizelgesi
```

## Database'de ne degisti

`credit_scores` tablosuna dokunduk. Hibernate `ddl-auto: update` ile yeni sema otomatik olustu:

| kolon         | tip       | not                              |
|---------------|-----------|----------------------------------|
| id            | bigint    | PK, otomatik artan (BIGSERIAL)   |
| user_id       | bigint    | FK -> users(id)                  |
| week_index    | bigint    | **YENI**, NOT NULL, hangi hafta  |
| score         | integer   | NOT NULL, o haftanin skoru       |
| risk_category | varchar   | NOT NULL                         |
| queried_at    | timestamp | satir yazilma ani                |

Eklediklerimiz:
- **`week_index` kolonu** (bigint, NOT NULL) — haftalik snapshot'i isaretler + siralamayi verir.
- **Unique constraint `(user_id, week_index)`** — ayni kullanicinin ayni haftasina iki satir giremez, DB seviyesinde zorlanir (son hakem).

### Neden `docker compose down -v` gerekti
`ddl-auto: update` var olan bir tabloya **NOT NULL + unique** kolon ekleyemez (eski satirlarda
`week_index` bos olurdu -> constraint patlar). Veriler mock oldugu icin **volume'u sildik**
(`docker compose down -v`) -> temiz DB -> Hibernate yeni semayi sifirdan kurdu.

**Not:** `down` (v'siz) volume'u SILMEZ, satirlar kalir. `down -v` volume'u siler -> veriler gider.
(Container gecici, veri named volume `pgdata`'da yasar.)

## Knobs (ayar dugmeleri)

- `REVERSION = 0.35` — yapiskanlik. Yuksek -> base'e daha cok yapisir, uclar daha nadir. Dusuk -> daha cok gezer.
- `volatility` (50..500, tcNo'dan) — noise genligi. Sakin kullanici vs zipzip kullanici farki.

## Caveat (kod yorumunda da var)

Snapshot uretimi bir **GET** (`my-score`) icinde oluyor -> yan etkili okuma, hafif koku.
Gercek sistemde bunu **haftalik zamanlanmis bir job** yapar; biz sorgu aninda tembel (lazy)
uretiyoruz. Mock icin yeterli. (`getHistory` ise uretim yapmaz, saf okuma.)

## Test sonucu (gercekten calistirdik)

Backend :8080'de ayaga kalkti, tablo dogru olustu, register/login/my-score/my-history calisti.
Backdate edilmis kullanicilarda haftalik walk base etrafinda **gezip geri cekildi**, 1 veya
1900'e yapismadi. Ayni hafta tekrar sorgu -> **kopya satir yok** (unique + insert-on-new-week).
