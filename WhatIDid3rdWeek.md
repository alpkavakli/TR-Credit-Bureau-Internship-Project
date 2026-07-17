# 3. Hafta — Ne Yaptım, Neden Yaptım

Bu dosya 3. hafta görev çizelgesindeki işlerin **yapılma sırasına göre** dökümü.
Her adımda: *problem neydi → ne değişti → hangi kavramı öğrendim*.

Hafta başındaki durum: `CreditScoreService`, `ScoreController` ve `GlobalExceptionHandler`'ın
ilk hali zaten vardı. Kalanlar bu hafta yapıldı.

---

## Sıra 1 — Exception katmanı

**Neden ilk bu?** `AdminController`'dan **önce** yapılması şarttı. Sebep:
`AccessDeniedException extends RuntimeException`. O sırada `GlobalExceptionHandler`'da
her `RuntimeException`'ı yakalayıp **400** döndüren tek bir handler vardı. Yani
`@PreAuthorize` bir isteği reddetseydi, **403 olması gereken cevap 400** olarak dönecekti.
Admin uçlarını bu haldeyken yazsak yetki testleri yanlış sonuç verirdi.

### Yeni: `exception/` paketi

- [NotFoundException.java](miniscore/backend/src/main/java/com/findeks/miniscore/exception/NotFoundException.java)
- [EmailAlreadyExistsException.java](miniscore/backend/src/main/java/com/findeks/miniscore/exception/EmailAlreadyExistsException.java)

İkisi de `RuntimeException`'dan türüyor ve gövdeleri boş. Çünkü tek görevleri
**bir isim olmak**. Ana fikir: *exception'ın tipi bilgidir*.
`throw new RuntimeException("e-posta zaten kayıtlı")` dersem bilgi metnin içinde
kalır ve handler ondan HTTP kodu çıkaramaz. Kendi tipim olursa tip başlı başına
"bu 409'dur" der.

### Değişen: [GlobalExceptionHandler.java](miniscore/backend/src/main/java/com/findeks/miniscore/controller/GlobalExceptionHandler.java)

Tek `RuntimeException → 400` handler'ı **silindi**. Yerine:

| Exception | Kod | Neden |
|---|---|---|
| `BadCredentialsException` | 401 | Şifre yanlış |
| `AuthenticationException` | 401 | Diğer kimlik doğrulama hataları |
| `AccessDeniedException` | 403 | Kimliğin belli ama yetkin yok |
| `EmailAlreadyExistsException` | 409 | İstek geçerli, kaynakla çakışıyor |
| `NotFoundException` | 404 | Kayıt yok |
| `MethodArgumentNotValidException` | 400 | `@Valid` patladı |
| `HttpMessageNotReadableException` | 400 | JSON hiç okunamadı *(sonradan eklendi, aşağıda)* |
| `Exception` | 500 | Son savunma hattı |

En önemli değişiklik: varsayılan artık **400 değil 500**. "Tanımadığım hata"
istemcinin suçu (400) değil, benim bugum (500) sayılıyor. Eskiden bir
`NullPointerException` kullanıcıya `400 {"message": null}` olarak gidiyordu.

Öğrendiğim ayrıntılar:
- Spring **en spesifik** handler'ı seçer; metot sırası önemli değil.
- 500 handler'ında `ex.getMessage()`'ı istemciye **vermiyoruz** ;içinde SQL, tablo
  adı, dosya yolu sızabilir. Stack trace sadece log'a gider.
- 401'de "e-posta yok" ile "şifre yanlış" ayrımını **kasten** yapmıyoruz; ayırsak
  saldırgan hangi e-postaların kayıtlı olduğunu öğrenirdi (user enumeration).

### Değişen: [AuthService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/AuthService.java), [CreditScoreService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/CreditScoreService.java)

Çıplak `RuntimeException`'lar yeni tiplerle değiştirildi. `CreditScoreService`'te
`findTopByUserOrderByWeekIndexDesc(...).orElseThrow()` **bilerek dokunulmadan bırakıldı**:
orası `generateMissingWeeks`'ten hemen sonra geliyor, boş dönerse gerçekten bizim
bugumuzdur ve 500 dönmesi doğrudur.

### Değişen: [SecurityConfig.java](miniscore/backend/src/main/java/com/findeks/miniscore/config/SecurityConfig.java)

Buradaki asıl ders: **filtre seviyesindeki 401/403 `@RestControllerAdvice`'a hiç ulaşmaz.**

Spring iki ayrı yerde reddediyor:

| Nerede | Ne zaman | Advice görür mü? |
|---|---|---|
| `ExceptionTranslationFilter` (filtre zinciri) | SecurityConfig'teki `.hasRole()`, eksik/geçersiz JWT | **Hayır** |
| `@PreAuthorize` (method security) | Controller metoduna girerken | **Evet** |

Filtreler `DispatcherServlet`'ten **önce** çalışır, `@RestControllerAdvice` ise onun
içinde yaşar. Bu yüzden token'sız istekte hata advice'a hiç uğramıyordu ve
`HttpStatusEntryPoint` **boş gövdeli** 401 döndürüyordu — Postman'de neden 401
aldığımı göremiyordum.

Çözüm: `HttpStatusEntryPoint` kaldırıldı, yerine iki bean geldi:
- `restAuthEntryPoint()` → 401 + JSON
- `restAccessDeniedHandler()` → 403 + JSON

İkisi de ortak `writeJson()` yardımcısını kullanıyor ve `ObjectMapper` ile aynı
`ErrorResponse` şeklini üretiyor → istemci hatanın filtreden mi controller'dan mı
geldiğini bilmek zorunda değil.

---

## Sıra 2 — Denetim günlükleme (audit logging)

**Problem:** `SCORE_QUERY` kaydı vardı ama `AuditLog.builder()...` `CreditScoreService`'in
içine gömülüydü. LOGIN/REGISTER için aynı kod `AuthService`'e kopyalanacaktı → aynı mantık
iki-üç yerde.

### Yeni: [AuditService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/AuditService.java)

Audit yazan **tek yer**. Yarın audit'e "IP adresi" eklenirse tek dosya değişir.

Buradaki asıl kavram **`@Transactional(propagation = Propagation.REQUIRES_NEW)`**:

> "Beni çağıran transaction ne olursa olsun, ben kendi transaction'ımı açarım."

Neden lazım: `queryScore()` `@Transactional`. Audit yazıldıktan sonra metodun
sonunda hata patlarsa tüm transaction geri alınır ve **audit kaydı da silinirdi**.
Ama denetim kaydının amacı tam da "ne oldu"yu tutmaktır; başarısız denemeler bile
iz bırakmalıdır. `REQUIRES_NEW` ile audit kendi başına commit olur.

⚠️ Spring tuzağı: bu ancak **başka bir bean'den** çağrılırsa çalışır. Aynı sınıf
içinden `this.log(...)` çağrılsaydı proxy devreye girmez, annotation **sessizce**
görmezden gelinirdi.

### Değişen: [AuthService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/AuthService.java)

- `REGISTER` audit'i eklendi.
- `LOGIN` audit'i eklendi.
- **`LOGIN_FAILED`** eklendi — `authenticate()` çağrısı `try/catch` içine alındı,
  `BadCredentialsException` yakalanıp loglanıyor ve **tekrar fırlatılıyor**
  (yutulmuyor; `GlobalExceptionHandler` onu 401'e çevirsin diye).
  Denetim açısından en değerli kayıt budur: arka arkaya 50 başarısız LOGIN = saldırı sinyali.

### Değişen: [CreditScoreService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/CreditScoreService.java)

`AuditLogRepository` bağımlılığı `AuditService` ile değiştirildi; inline
`AuditLog.builder()` yerine tek satır `auditService.log(...)`.
**Skor üretme algoritmasına dokunulmadı.**

### Değişen: [User.java](miniscore/backend/src/main/java/com/findeks/miniscore/entity/User.java) — `@PrePersist`

Çizelgedeki `@PrePersist` kavramı burada. `@CreationTimestamp`'ten farkı:

| | `@CreationTimestamp` | `@PrePersist` |
|---|---|---|
| Kimin | Hibernate'e özel | **Standart JPA** |
| Ne yapar | Sadece tarih basar | İçine **istediğin kodu** yazarsın |
| Ne zaman | INSERT anında | INSERT'ten hemen önce (`@PreUpdate` → UPDATE öncesi) |

Somut iş: e-postayı küçük harfe çevirmek. `"Alp@Test.com"` ile `"alp@test.com"` aynı
kişidir ama DB'nin unique kısıtı büyük/küçük harfe duyarlı → normalize etmezsek
aynı adresle iki hesap açılabilirdi.

`Locale.ROOT` kullanmak **şart**: Türkçe locale'de `"I".toLowerCase()` → `"ı"` olur
ve e-postayı bozar.

Entity seviyesine koydum çünkü kaydın hangi yoldan geldiği (register, seeder)
fark etmeksizin kural yeni kayıtlarda hep uygulansın. `AuthService`'te de sorgudan
önce aynı normalizasyon var — çünkü `existsByEmail` **sorgusu** da aynı formatta
olmalı, yoksa `"Alp@Test.com"` ile arayıp mevcut kaydı bulamazdık.

> ⚠️ **Bu adımda `@PreUpdate` de eklemiştim ve bug çıktı — Sıra 7'de anlatılıyor.**
> Kural artık `@PrePersist` (sadece INSERT) + servis katmanı ile uygulanıyor.
> Ortak kural [EmailUtils.java](miniscore/backend/src/main/java/com/findeks/miniscore/common/EmailUtils.java)'da.

> Not: Bu kural **yeni** kayıtlar için geçerli. DB'deki eski test kullanıcıları
> (`Ayse@test.com`, `Can@test.com`, `Deniz@test.com`) büyük harfli kaldı.

---

## Sıra 3 — Admin bootstrap

**Problem:** `register()` herkesi `Role.USER` yapıyor → sistemde **ADMIN olma yolu yok**
→ `AdminController` test edilemez. Klasik "bootstrap" problemi.

Seçenekler ve neden bu:
1. `register()`'a `role` alanı eklemek → **felaket**, herkes kendini admin yapardı.
2. DB'ye elle SQL ile admin basmak → çalışır ama her yeni ortamda elle iş.
3. **Seeder** → seçilen bu.

### Yeni: [AdminSeeder.java](miniscore/backend/src/main/java/com/findeks/miniscore/config/AdminSeeder.java)

`CommandLineRunner` implement ediyor → Spring Boot uygulama tamamen ayağa kalktıktan
**hemen sonra** `run()`'ı bir kez çalıştırır (bean'ler hazır, repository kullanılabilir).

- `@Value("${app.admin.email:admin@findeks.com}")` → `:` sonrası **varsayılan** değer,
  yml'de yoksa uygulama patlamaz.
- **Idempotent**: her açılışta çalışır, `existsByEmail` ile kontrol edip ikinci kez
  yaratmaz. (Yaratmaya kalksa unique kısıt hata verir ve uygulama açılmazdı.)
- Şifre log'a **yazılmaz**.

### Değişen: [application.yml](miniscore/backend/src/main/resources/application.yml)

```yaml
app:
  admin:
    email: admin@findeks.com
    password: admin12345
    tc-no: "00000000000"     # tırnak şart: tırnaksız YAML bunu 0 sayısına çevirirdi
```

Gerçek projede bu şifre yml'de değil ortam değişkeninde / secret manager'da durur.

---

## Sıra 4 — Admin DTO → AdminService → AdminController

### Yeni DTO'lar

- [UserResponse.java](miniscore/backend/src/main/java/com/findeks/miniscore/dto/UserResponse.java) — **password YOK, creditScores YOK** (kasten)
- [AuditLogResponse.java](miniscore/backend/src/main/java/com/findeks/miniscore/dto/AuditLogResponse.java)
- [UpdateRoleRequest.java](miniscore/backend/src/main/java/com/findeks/miniscore/dto/UpdateRoleRequest.java)

`UserResponse`'ta entity yerine DTO döndürmenin üç sebebi:
1. **Güvenlik**: `User.password` var (hash olsa bile). Entity dönsek Jackson onu
   JSON'a yazardı → tüm hash'ler API'den sızardı.
2. **Lazy loading**: `User.creditScores` `@OneToMany(LAZY)`. Jackson serialize
   ederken o listeye dokunur → `LazyInitializationException` ya da her kullanıcı
   için ekstra SQL.
3. **Sözleşme**: Entity DB'nin şekli, DTO API'nin şekli. Kolon adı değişince
   frontend kırılmasın diye ikisi ayrı durur.

`UpdateRoleRequest.role` tipi `String` değil **`Role` enum'u**: Jackson `"ADMIN"`
metnini enum'a çevirir, `"MUDUR"` gibi tanımsız değer controller'a **girmeden**
reddedilir. Yani geçersiz rol ihtimalini kendi `if`'imle değil **tip sistemine** yaptırdım.

### Yeni: [AdminService.java](miniscore/backend/src/main/java/com/findeks/miniscore/service/AdminService.java)

Katman kuralı: controller = HTTP'yi bilir, service = işi bilir, repository = DB'yi bilir.
Controller repository'ye uzanırsa iş mantığı HTTP katmanına sızar.

- `listUsers`, `getUser`, `listAuditLogs` → `@Transactional(readOnly = true)`
- `updateRole` → `@Transactional`, `ROLE_CHANGE` audit'i yazar.
  `setRole()` sonrası `save()` **yazmadım**: transaction içinde okunan entity
  "managed" durumdadır, Hibernate değişikliği görüp UPDATE'i kendi atar (dirty checking).
- `performedBy` controller'dan parametre olarak geçiliyor — "kim yaptı" oturumun
  bilgisi, iş mantığının değil; servis `SecurityContext`'e uzanmasın diye.

### Yeni: [AuditLogRepository](miniscore/backend/src/main/java/com/findeks/miniscore/repository/AuditLogRepository.java) — derived query

```java
List<AuditLog> findAllByOrderByCreatedAtDesc();
```

Gövdesini yazmıyorum; Spring Data metodun **adını** okuyup SQL'i üretiyor.
İsim alan adıyla birebir uyuşmalı — `creationDate` yazsaydım uygulama **açılırken**
hata verirdi (çalışma anında değil), yani hatayı erken görürdüm.

### Yeni: [AdminController.java](miniscore/backend/src/main/java/com/findeks/miniscore/controller/AdminController.java)

| Uç | Metot |
|---|---|
| `/api/admin/users` | GET |
| `/api/admin/users/{id}` | GET |
| `/api/admin/audit-logs` | GET |
| `/api/admin/users/{id}/role` | PUT |

Rol değiştirmede **PUT** kullandım çünkü işlem idempotent: aynı isteği 10 kez
göndersen sonuç aynı. POST "her çağrı yeni bir şey yaratır" demektir.

**İki katlı koruma (defense in depth):**

1. **URL seviyesi** — `SecurityConfig`'teki `.requestMatchers("/api/admin/**").hasRole("ADMIN")`.
   Filtre zincirinde, controller'a girmeden çalışır. Kaba ama sağlam: yeni bir admin
   metodu eklerken korumayı unutsam bile kural geçerli.
2. **Metot seviyesi** — sınıf üstündeki `@PreAuthorize("hasRole('ADMIN')")`.
   AOP proxy'si ile metot çağrılmadan önce çalışır, ince ayar yapılabilir
   (`"hasRole('ADMIN') or #id == authentication.principal.id"` gibi).

⚠️ **En kritik tuzak:** `@PreAuthorize` için `@EnableMethodSecurity` **şart**.
Yoksa Spring annotation'ı **sessizce** görmezden gelir — hata vermez, uygulama
açılır, ama admin uçları korumasız kalır. En tehlikeli hata türü bu: görünürde
her şey yolunda. `SecurityConfig`'e eklendi.

`hasRole('ADMIN')` aslında `"ROLE_ADMIN"` yetkisini arar, `"ROLE_"` önekini Spring
kendisi ekler. `User.getAuthorities()` de `"ROLE_" + role.name()` döndürüyor → uyumlu.
(`hasAuthority('ADMIN')` yazsaydım **eşleşmezdi**.)

---

## Sıra 5 — DTO süpürgesi

Kontrol edildi: **hiçbir controller entity döndürmüyor.**

| Controller | Döndürdüğü |
|---|---|
| `AuthController` | `AuthResponse` |
| `ScoreController` | `CreditScoreResponse` |
| `AdminController` | `UserResponse`, `AuditLogResponse` |

**Tek yönlü eşleşme**: `User → UserResponse` çevirisi var, tersi **yok**. Dışarıdan
gelen veri asla doğrudan entity'ye dönüşmez — o iş `RegisterRequest`/`UpdateRoleRequest`'in.

---

## Sıra 6 — Postman koleksiyonu

**Önceki sorunlar:**
- `register.request.yaml` URL'inin **başında boşluk** vardı → `%20` gönderiliyor →
  `permitAll` kuralıyla eşleşmiyor → sebepsiz 401. Silindi.
- Token'lar dosyaya **elle yapıştırılmıştı** → 24 saatte ölüyordu.

**Çözüm:** [definition.yaml](postman/collections/New%20Collection/.resources/definition.yaml)'a
koleksiyon değişkenleri eklendi: `{{baseUrl}}`, `{{token}}`, `{{adminToken}}`.
`login` ve `admin-login` isteklerinin `afterResponse` script'i token'ı otomatik yazıyor.

`{{token}}` ve `{{adminToken}}` **ayrı** tutuluyor — aynı yerde olsalardı
"USER admin ucuna girebiliyor mu?" testini yapamazdık.

| İstek | Ne test ediyor |
|---|---|
| `register` | 201 / 409 |
| `login` | 200 + `{{token}}` doldurur |
| `admin-login` | 200 + rol ADMIN + `{{adminToken}}` doldurur |
| `tokensiz-401` | 401 **ve gövde boş değil** |
| `kullanıcı-skor` | 200 + skor 1–1900 arası |
| `kullanıcı-gecmis` | 200 + haftalar artan sırada |
| `admin-kullanicilar` | 200 + **password ve creditScores dönmemeli** |
| `admin-kullanici-detay` | 200 |
| `admin-kullanici-yok-404` | 404 |
| `admin-yetkisiz-403` | **403** (USER token'ı ile) |
| `admin-audit-logs` | 200 + createdAt DESC |
| `admin-rol-degistir` | 200 + rol değişti |
| `admin-gecersiz-rol-400` | 400 (500 değil) |

API sözleşmesi sadece "mutlu yol"dan ibaret değil — **hata kodları da sözleşmenin parçası**,
o yüzden negatif testler var.

---

## Sıra 7 — Kod incelemesi ve düzeltmeler

Her şey bittikten sonra diff'in tamamı ayrıca gözden geçirildi. Çıkan 9 bulgudan
**üçü düzeltildi**, kalanlar aşağıda borç olarak duruyor.

### Düzeltme 1 — `@PreUpdate` kaldırıldı (bulunan en sinsi bug)

`User`'a `@PrePersist` **ve** `@PreUpdate` birlikte konmuştu: "kural her yerde
geçerli olsun" diye. Bedeli sonradan görüldü.

`@PreUpdate` entity'nin **her** güncellemesinde çalışır — güncellemenin e-postayla
ilgisi olmasa bile. Test:

```
PUT /api/admin/users/4/role   {"role":"ADMIN"}
→ rol değişti  ...ama e-posta da değişti:  Ayse@test.com → ayse@test.com
```

Üç sonucu vardı:
1. Alakasız bir işlem (rol değişimi) kullanıcının **giriş kimliğini** değiştiriyordu.
2. O kullanıcının elindeki JWT'nin `sub`'ı hâlâ eski e-posta →
   `JwtAuthFilter.loadUserByUsername` büyük/küçük harfe duyarlı `findByEmail` ile
   bulamaz → `UsernameNotFoundException` → filtre sadece `JwtException` yakaladığı
   için exception dışarı sızar → **401 yerine 500**.
3. DB'de hem `X@a.com` hem `x@a.com` olsaydı → unique kısıt ihlali → 500.

**Ders:** lifecycle callback'i "her ihtimale karşı" geniş tutmak bedava değil.
Yazdığı veri, o güncellemeyi isteyen kodun **haberi olmadan** değişiyor.
`@PreUpdate` silindi; artık `@PrePersist` (sadece INSERT) + servis katmanı.

Doğrulama: `PUT /users/5/role` sonrası `Can@test.com` **aynı kaldı** ✅

### Düzeltme 2 — Seeder aslında idempotent değildi

`AdminSeeder` `existsByEmail(adminEmail)`'i **ham** değerle çağırıyordu, ama
`@PrePersist` DB'ye küçük harfle yazıyordu. `app.admin.email: Admin@Findeks.com`
yazılsaydı:

| Açılış | Ne olurdu |
|---|---|
| 1. | `existsByEmail("Admin@Findeks.com")` → false → insert (DB'ye `admin@findeks.com` girer) |
| 2. | yine false (aradığımız büyük harfli) → tekrar insert → **unique ihlali → uygulama hiç açılmaz** |

Kural `AuthService`'te vardı, seeder'a eklemeyi unutmuştum. **Arama ile yazma aynı
biçimde olmalı.**

### Yeni: [EmailUtils.java](miniscore/backend/src/main/java/com/findeks/miniscore/common/EmailUtils.java)

Düzeltme 2'nin kök sebebi: aynı kural **iki yere kopyalanmıştı** (`AuthService.normalize`
ve `User.@PrePersist`), üçüncü yerde (seeder) unutuldu. Kural tek bir yere alındı;
`AuthService`, `User` ve `AdminSeeder` artık aynı metodu çağırıyor.

**Ders:** aynı kural birden fazla yerde tekrarlanınca, er ya da geç biri geride kalır.
Bug'ı düzeltmek yetmez, **tekrarı** düzeltmek gerekir.

### Düzeltme 3 — Son admin koruması

`updateRole`'de hiçbir koruma yoktu. Tek admin kendi rolünü USER yaparsa:
sistemde hiç ADMIN kalmıyor → `/api/admin/**` herkese kapanıyor →
**`AdminSeeder` de kurtarmıyor**, çünkü o sadece "bu e-posta var mı" diye bakıyor,
**rolüne bakmıyor** → "zaten mevcut" deyip çıkıyor. Geri dönüşün tek yolu elle SQL.

Eklenenler:
- [UserRepository.countByRole()](miniscore/backend/src/main/java/com/findeks/miniscore/repository/UserRepository.java) —
  derived query, `SELECT COUNT(*)` DB'de çalışır (tüm kullanıcıları çekip Java'da
  saymaktan ucuz).
- [LastAdminException.java](miniscore/backend/src/main/java/com/findeks/miniscore/exception/LastAdminException.java) → **409**.
  400 değil (istek kusursuz), 403 değil (yetki var) → sistemin **mevcut durumu** izin vermiyor.
- Kontrol `@Transactional` içinde: sayım ile güncelleme aynı transaction'da.

Kontrolün yeri **servis**, controller değil — bu bir **iş kuralı**.

Doğrulama: tek admin kendini USER yapmayı denedi → **409** ✅; ikinci admin
varken aynı işlem → **200** ✅ (koruma gereksiz yere engellemiyor)

---

## Test sırasında bulunan gerçek bug

Uygulamayı çalıştırıp uçları tek tek denerken çıktı:

**`{"role":"MUDUR"}` → 500 dönüyordu, 400 dönmeliydi.**

Sebep: Jackson `"MUDUR"`'u `Role` enum'una çeviremiyor →
`HttpMessageNotReadableException` fırlatıyor. Bu `@Valid`'den **önce** oluyor
(Jackson daha nesneyi kuramıyor), o yüzden `MethodArgumentNotValidException`
**değil**. Handler'da karşılığı olmadığı için `Exception → 500`'e düşüyordu.

İstemcinin gönderdiği bozuk JSON **5xx olmamalı**. `GlobalExceptionHandler`'a
özel handler eklendi → artık 400. `admin-gecersiz-rol-400` isteği bunu koruyor.

Ders: derlenmesi çalıştığı anlamına gelmiyor. Bu bug'ı sadece **uygulamayı
çalıştırıp gerçek istek atarak** gördüm.

---

## Doğrulama (uygulama çalışır haldeyken)

| Test | Sonuç |
|---|---|
| Token'sız `/api/scores/my-score` | 401 `{"message":"Giriş yapmanız gerekiyor."}` ✅ |
| Geçersiz token | 401 `{"message":"Geçersiz token."}` ✅ |
| Yanlış şifre | 401 `{"message":"E-posta veya şifre hatalı."}` ✅ |
| `ADMIN@Findeks.COM` ile login | 200 ✅ (normalizasyon çalışıyor) |
| Aynı e-posta ile register | 409 ✅ |
| Eksik alanlı register | 400 + alan alan hata listesi ✅ |
| ADMIN → `/api/admin/users` | 200, **password yok, creditScores yok** ✅ |
| **USER → `/api/admin/users`** | **403** ✅ |
| Olmayan id | 404 ✅ |
| Rol USER→ADMIN→USER | 200 + `ROLE_CHANGE` audit ✅ |
| Geçersiz rol | 400 ✅ (düzeltmeden sonra) |
| Başarısız login sonrası audit | `LOGIN_FAILED` kaydı **var** ✅ |
| Uygulamayı 2. kez açma | "Admin kullanıcısı zaten mevcut" ✅ (idempotent) |
| `BUYUK@Harf.COM` ile register | DB'ye `buyuk@harf.com` ✅ (`@PrePersist` kanıtı) |
| Rol değişimi sonrası e-posta | **değişmiyor** ✅ (`@PreUpdate` düzeltmesi) |
| Son admin kendini USER yapmayı denedi | **409** ✅ |
| İki admin varken rol düşürme | 200 ✅ (koruma gereksiz engellemiyor) |

`LOGIN_FAILED` kaydının exception fırlamasına rağmen durması `REQUIRES_NEW`'in
çalıştığının kanıtı.

> Not: `@PrePersist`'i ilk testlerde **hiç doğrulayamamıştım** — o turda başarılı
> hiç `register` olmamıştı (hep 409/400 dönmüştü), yani callback hiç çalışmamıştı.
> "Test ettim" ile "test o kod yolundan geçti" ayrı şeyler.

---

## Çizelge durumu

| Görev | Durum |
|---|---|
| CreditScoreService (`@Transactional`) | ✅ (bu hafta dokunulmadı) |
| Denetim günlükleme (`@CreationTimestamp`, `@PrePersist`) | ✅ |
| ScoreController (`@AuthenticationPrincipal`) | ✅ |
| AdminController (`@PreAuthorize`, `hasRole()`) | ✅ |
| DTO dönüşümü (tek yönlü eşleşme) | ✅ |
| GlobalExceptionHandler (`@ExceptionHandler`) | ✅ |
| Postman koleksiyonu (API sözleşmesi) | ✅ |

---

## Açık kalanlar / borç

### Kod incelemesinden çıkan, bilerek ertelenenler

Bunlar gerçek bulgular — "yok" değil, "şimdilik kabul edildi". Sırayla ele alınmalı.

**1. `REQUIRES_NEW` + connection havuzu (en riskli)**
`queryScore()` `@Transactional` olduğu için 1 connection tutuyor, sonra
`auditService.log()` ikincisini istiyor. Hikari varsayılan havuzu **10**.
10 eşzamanlı `/my-score` isteği hepsi 1. connection'ı alıp 2.'yi beklerse havuzda
boş connection kalmaz → hiçbiri ilerleyemez → istekler timeout'a kadar asılı kalır.
Çözüm: havuzu büyütmek ya da audit'i `@TransactionalEventListener(AFTER_COMPLETION)`
ile transaction'dan tamamen ayırmak. (`ConcurrencySafety-WhatILearned.md` ile aynı konu.)

**2. `register`'da yarış durumu (TOCTOU)**
`existsByEmail` → `save` arasında boşluk var. Aynı e-postayla iki eşzamanlı istek:
ikisi de `false` görür, ikisi de save eder, biri unique kısıta takılır →
`DataIntegrityViolationException` → handler'da karşılığı yok → **409 yerine 500**.
Çözüm: `DataIntegrityViolationException`'ı yakalayıp 409'a çevirmek — yani
`CreditScore`'daki *"DB son hakem"* notunda yazdığım yaklaşımın aynısı.

**3. `audit-logs` sayfalamasız**
`findAllByOrderByCreatedAtDesc()` sınırsız. `audit_logs` her login/skor sorgusunda
büyüyor ve hiç silinmiyor. 100 bin satırda `GET /api/admin/audit-logs` hepsini
çeker, 100 bin DTO kurar, tek JSON'da döner → yanıt saniyelere çıkar, OOM riski.
Çözüm: `Pageable` alan `findAll(Pageable)`.

**4. `JwtAuthFilter` hâlâ JSON'u elle kuruyor**
`SecurityConfig.writeJson` `ObjectMapper` + `ErrorResponse` kullanırken
[JwtAuthFilter](miniscore/backend/src/main/java/com/findeks/miniscore/security/JwtAuthFilter.java)
`"{\"message\":\"...\"}"` string'i birleştiriyor. `ErrorResponse`'a alan eklenirse
(ör. `timestamp`) filtre cevabı diğerlerinden farklı kalır. `writeJson` ortak bir
bileşene çıkarılıp iki yerden de çağrılmalı. (`EmailUtils`'te yaptığımın aynısı.)

**5. Postman'de id=1 sabit kodlu**
`admin-rol-degistir` ve `admin-kullanici-detay` id=1'e bağlı. `docker compose down -v`
sonrası seeder admin'i id=1 yapar → `admin-rol-degistir` admin'in kendi rolünü
düşürmeye çalışır (artık 409 alır, ama test yine kırmızı olur). id, `admin-kullanicilar`
yanıtından script ile alınmalı.

**6. Postman değişken şeması doğrulanmadı**
`definition.yaml`'daki `variable:` anahtarının bu dosya formatında (collections-as-files)
doğru olduğu **Postman'de açılarak teyit edilmedi** — ortamda Postman yok.
Yanlışsa `{{baseUrl}}`/`{{token}}` çözülmez ve tüm istekler patlar. **İlk iş bunu aç ve bak.**

### Daha küçük borçlar

- `SecurityConfig`'teki `userDetailsService` alanı hiçbir yerde okunmuyor (ölü kod,
  benden önce de vardı). Zararsız ama silinebilir.
- `LocalDate.now()` hâlâ `CreditScoreService`'e gömülü → zaman test edilemiyor.
  İleride `Clock` inject edilebilir.
- Eski test kullanıcılarının e-postaları büyük harfli kaldı (`@PrePersist` sadece
  **yeni** kayıtlara işler; `@PreUpdate` bilerek kaldırıldı).
- `User` ve `CreditScore`'da `@Data` var → `equals`/`hashCode` iki yönlü ilişkiyi
  (`User.creditScores` ↔ `CreditScore.user`) dolaşır; sonsuz özyineleme ve lazy
  yükleme riski. `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` ile id'ye
  indirgenmeli. (Benden önce de vardı.)
- `common/apputil.java` boş ve `package` satırı yok.
- Admin şifresi `application.yml`'de; ortam değişkenine taşınmalı.
- `LOGIN_FAILED` sadece `BadCredentialsException` için yazılıyor; hesap kilitli/pasif
  gibi diğer `AuthenticationException` türleri loglanmıyor.
- `hs_err_pid*.log` ve `miniscore/backend/app.log` git'te takipsiz duruyor →
  `.gitignore`'a eklenmeli.
