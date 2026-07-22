# Findeks MiniScore — İyileştirmeler ve Ek Özellikler

Bu belge, temel proje (kayıt/giriş, skor, admin, React arayüz) tamamlandıktan sonra
yapılan **hata düzeltmeleri** ile dokümandaki **7. Ek Geliştirme Görevleri** kapsamında
eklenen özellikleri özetler.

**Doğrulama:** Backend `mvnw clean test` → derleme + birim testleri **başarılı (exit 0)**.
Frontend `npm run build` → prod derlemesi **başarılı (exit 0)**.

---

## 1. Hata Düzeltmeleri (Frontend)

### 1.1 Axios interceptor — 401'de bayat veri kalıyordu
**Dosya:** [frontend/src/api/axiosInstance.js](frontend/src/api/axiosInstance.js)

- **Önce:** 401 alınınca yalnızca `token` siliniyordu; `email`, `role` (ve şimdi
  `refreshToken`) `localStorage`'da kalıyordu.
- **Sonra:** Ortak `forceLogout()` → `localStorage.clear()` (logout ile aynı davranış)
  ve zaten `/login`'deysek gereksiz yönlendirme yapılmıyor.

### 1.2 403 (yetki yok) artık oturumu kapatmıyor
- **Önce:** Interceptor yalnızca 401'i ele alıyordu; rolü değişen bir kullanıcının
  admin çağrıları 403 dönünce sayfa ham hata gösteriyordu.
- **Sonra:** 403 için oturum **kapatılmıyor** — sayfa kendi hata mesajını gösterir.
  Sadece 401 (token süresi/bozuk) refresh akışını tetikler.

---

## 2. Ek Özellik #1 — Refresh Token (rotation'lı)

Kısa ömürlü access JWT'yi, uzun ömürlü ama **iptal edilebilir** bir refresh token ile
yeniliyoruz.

**Backend**
- [entity/RefreshToken.java](backend/src/main/java/com/findeks/miniscore/entity/RefreshToken.java) — DB'de tutulan opak UUID (JWT değil → silinerek iptal edilebilir).
- [repository/RefreshTokenRepository.java](backend/src/main/java/com/findeks/miniscore/repository/RefreshTokenRepository.java)
- [service/RefreshTokenService.java](backend/src/main/java/com/findeks/miniscore/service/RefreshTokenService.java) — `create`, `verifyAndRotate` (eskisini sil + yenisini ver → tek kullanımlık), `deleteAllForUser`.
- [dto/RefreshRequest.java](backend/src/main/java/com/findeks/miniscore/dto/RefreshRequest.java), [exception/InvalidRefreshTokenException.java](backend/src/main/java/com/findeks/miniscore/exception/InvalidRefreshTokenException.java) (→ 401).
- `AuthResponse`'a `refreshToken` alanı; `register`/`login` artık refresh de üretir;
  yeni uç: **`POST /api/auth/refresh`**.
- `application.yml` → `jwt.refresh-expiration: 604800000` (7 gün).

**Frontend**
- `axiosInstance.js` 401 alınca `refreshToken` ile **sessizce** yeni access token alır,
  orijinal isteği bir kez tekrar dener; başarısızsa çıkış yapar. Eşzamanlı 401'ler için
  tek-sefer refresh kilidi (`refreshing`) var.
- `AuthContext.login()` refresh token'ı saklar; `authService.refresh()` eklendi.

---

## 3. Ek Özellik #2 — Swagger UI / OpenAPI

- `pom.xml` → `springdoc-openapi-starter-webmvc-ui` (2.8.9, Spring Boot 3.5 uyumlu).
- [config/OpenApiConfig.java](backend/src/main/java/com/findeks/miniscore/config/OpenApiConfig.java) — `bearerAuth` şeması (Swagger'daki "Authorize" ile JWT girilebilir).
- `SecurityConfig` → `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` herkese açık.
- **Adres:** `http://localhost:8080/swagger-ui.html`

---

## 4. Ek Özellik #6 — Profil Güncelleme

- [dto/UpdateProfileRequest.java](backend/src/main/java/com/findeks/miniscore/dto/UpdateProfileRequest.java) — yalnızca ad/soyad (e-posta = giriş kimliği, tcNo = skor çekirdeği olduğu için **bilerek** değiştirilmez).
- [service/UserService.java](backend/src/main/java/com/findeks/miniscore/service/UserService.java), [controller/UserController.java](backend/src/main/java/com/findeks/miniscore/controller/UserController.java) — **`GET /api/users/me`**, **`PUT /api/users/me`** (`@AuthenticationPrincipal` → yalnızca kendi profili). Güncelleme `PROFILE_UPDATE` olarak denetime yazılır.
- **Frontend:** [pages/ProfilePage.jsx](frontend/src/pages/ProfilePage.jsx), `userService.js`, `/profile` rotası, `Layout`'a "profil" bağlantısı.

---

## 5. Ek Özellik #3 — Sayfalama (Pagination)

- `AuditLogRepository.findAllByOrderByCreatedAtDesc(Pageable)` → `Page<AuditLog>`.
- `AdminService.listAuditLogs(Pageable)` → `Page<AuditLogResponse>` (`Page.map` ile metadata korunur).
- `AdminController` → `@PageableDefault(size = 20, sort = "createdAt", DESC)`; yanıt
  `{ content, totalPages, number, ... }`.
- **Frontend:** `adminService.listAuditLogs(page, size)`; `AdminPage` denetim kayıtlarına
  "‹ önceki / sonraki ›" ile sayfa gezinme.

---

## 6. Ek Özellik #5 — Skor Grafiği (recharts)

- `package.json` → `recharts`.
- [pages/ReportPage.jsx](frontend/src/pages/ReportPage.jsx) — geçmiş verisinden `LineChart`
  (x = hafta başlangıcı, y = skor, 0–1900). Tablo aynen korundu; grafik üstüne eklendi.

---

## 7. Ek Özellik #4 — Birim Testleri (Mockito)

- [test/.../AuthServiceTest.java](backend/src/test/java/com/findeks/miniscore/service/AuthServiceTest.java) — kayıt (token+refresh döner), e-posta çakışması (kaydetmez), hatalı girişte `LOGIN_FAILED` loglar + yeniden fırlatır.
- [test/.../AdminServiceTest.java](backend/src/test/java/com/findeks/miniscore/service/AdminServiceTest.java) — **son admin düşürülemez** (`LastAdminException`), kullanıcı ADMIN yapılır + denetime yazılır.
- `@SpringBootTest` yerine `@ExtendWith(MockitoExtension.class)`: DB gerektirmez, CI'da
  saniyeler yerine milisaniyede koşar.

---

## 8. Ek Özellik #7 — Tam Docker Yığını (Nginx)

- [backend/Dockerfile](backend/Dockerfile) — çok aşamalı (maven build → JRE).
- [frontend/Dockerfile](frontend/Dockerfile) — çok aşamalı (node build → nginx),
  [frontend/nginx.conf](frontend/nginx.conf) SPA fallback (`try_files ... /index.html`).
- `.dockerignore` dosyaları (backend/frontend).
- [docker-compose.yml](../docker-compose.yml) — **düzeltme:** `backend` build yolu `./backend`
  (hiç var olmayan yol) → `./miniscore/backend`. **Yeni:** `frontend` servisi
  (`3000:80`, `depends_on: backend`).
- `SecurityConfig` CORS listesine `http://localhost:3000` (Nginx origin) eklendi.
- **Çalıştırma:** `docker compose up --build` → db + backend + frontend
  (arayüz `http://localhost:3000`).

---

## 9. Admin ↔ Findeks Raporu Kuralları

**"Yöneticinin findeks raporu olmaz; ama başkalarınınkini görebilir."**

**Backend**
- [exception/ScoreNotAllowedException.java](backend/src/main/java/com/findeks/miniscore/exception/ScoreNotAllowedException.java) → 403.
- `CreditScoreService` — `queryScore` ve `getHistory` artık `ensureHasScore()` ile
  ADMIN'i reddeder. Yeni `getHistoryForUser(User)` admin görüntülemesi için eksik
  haftaları üretip tam geçmişi döner.
- `AdminService.getUserScores(id, adminEmail)` — hedef ADMIN ise reddeder; değilse
  geçmişi döner ve **`ADMIN_SCORE_VIEW`** olarak denetime yazar (kim baktı izi).
- Yeni uç: **`GET /api/admin/users/{id}/scores`**.

**Frontend**
- `Layout` — "skorum"/"geçmiş" menüsü yalnızca USER'a görünür.
- `ProtectedRoute` `blockRole` prop'u; `/dashboard` ve `/reports` ADMIN'e kapalı
  (→ `/admin`). Kök yol (`/`) role göre yönlendirir (`HomeRedirect`).
- `AdminPage` — her USER satırında "skorları gör": güncel skor kartı + skor grafiği
  (recharts) + hafta/skor/risk tablosu. ADMIN satırlarında findeks sütunu "—".

---

## 10. E-posta ile 2 Adımlı Doğrulama (2FA)

**Admin panelinden açılıp kapatılabilen, giriş ve kayıtta e-posta kodu isteyen 2FA.**

**Backend**
- Ayar DB'de: [entity/AppSetting.java](backend/src/main/java/com/findeks/miniscore/entity/AppSetting.java) + [service/SettingService.java](backend/src/main/java/com/findeks/miniscore/service/SettingService.java) (`email_2fa_enabled`, varsayılan **kapalı**).
- Kod: [entity/VerificationCode.java](backend/src/main/java/com/findeks/miniscore/entity/VerificationCode.java) (BCrypt hash, süreli, deneme sınırlı) + [service/VerificationService.java](backend/src/main/java/com/findeks/miniscore/service/VerificationService.java).
- Gönderim: [service/EmailService.java](backend/src/main/java/com/findeks/miniscore/service/EmailService.java) (Gmail SMTP, `spring-boot-starter-mail`).
- Akış: `register`/`login` artık **`AuthStepResponse`** döner. 2FA açık ve kullanıcı ADMIN
  değilse token verilmez → kod e-postayla gider → **`POST /api/auth/verify`** ile token alınır.
- **ADMIN muaf** (`twoFactorRequiredFor`): `admin@findeks.com` gerçek kutu değil; muaf olmasa
  2FA açılınca admin kilitlenirdi.
- Admin uçları: **`GET /api/admin/settings`**, **`PUT /api/admin/settings/email-2fa`** (audit'e `SETTING_CHANGE`).
- SMTP kimlik bilgileri **ortam değişkeninden** (`MAIL_USERNAME`/`MAIL_PASSWORD`), git'e girmez.
- Yeni hatalar: `InvalidVerificationCodeException` (400 — bilerek 401 değil), e-posta gönderim
  hatası → 503.

**Frontend**
- [components/EmailVerify.jsx](frontend/src/components/EmailVerify.jsx) — kod giriş ekranı; Login ve
  Register `verificationRequired` dönünce bunu gösterir, doğrulanınca oturumu açar.
- `AdminPage` — "Sistem Ayarları" bölümünde 2FA **Aç/Kapat** düğmesi + durum.

> Kurulum (Gmail App Password + env) README'de: "E-posta ile 2 adımlı doğrulama".

---

## Yeni / Değişen Uç Noktalar (özet)

| Metot | Yol | Not |
|------|-----|-----|
| POST | `/api/auth/verify` | E-posta 2FA kodunu doğrula → token |
| GET/PUT | `/api/admin/settings[/email-2fa]` | 2FA aç/kapat (admin) |
| POST | `/api/auth/refresh` | Yeni access + yeni refresh (rotation) |
| GET  | `/api/users/me` | Kendi profilini getir |
| PUT  | `/api/users/me` | Ad/soyad güncelle |
| GET  | `/api/admin/users/{id}/scores` | Bir kullanıcının skor geçmişi (admin) |
| GET  | `/api/admin/audit-logs` | Artık **sayfalı** (`?page=&size=`) |
| GET  | `/swagger-ui.html` | API dokümantasyonu |

## Kalan (opsiyonel) not
- `recharts` frontend paketini ~620 kB'a çıkarıyor (sadece bir uyarı). İstenirse
  `manualChunks` ile kod bölme yapılabilir; işlev için gerekmiyor.
