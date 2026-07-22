# Findeks MiniScore

JWT tabanlı kimlik doğrulaması olan bir kredi skoru (mock) uygulaması.
**Backend:** Spring Boot 3.5 + PostgreSQL · **Frontend:** React (Vite).

- Backend kaynak: `miniscore/backend`
- Frontend kaynak: `miniscore/frontend`
- Ek özellikler / değişiklikler: [miniscore/IYILESTIRMELER.md](miniscore/IYILESTIRMELER.md)

---

## Gereksinimler

| Araç | Sürüm |
|------|-------|
| Docker Desktop | son sürüm |
| Java JDK | 17 |
| Node.js | 18+ |

> Aşağıdaki tüm komutlar **repo kök dizininden** (`FindeksRaporuStaj/`) çalıştırılır,
> aksi belirtilmedikçe.

---

## Yol A — Tamamı Docker ile (en kolay)

Veritabanı + backend + frontend, hepsi tek komutla ayağa kalkar:

```bash
docker compose up --build
```

Açılınca:

| Servis | Adres |
|--------|-------|
| Frontend (Nginx) | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

Durdurmak için: `Ctrl+C`, ardından `docker compose down`
(verileri de silmek için: `docker compose down -v`).

---

## Yol B — Yerel geliştirme (hot-reload)

Geliştirirken bu yol daha pratiktir: sadece **veritabanı** Docker'da koşar,
backend ve frontend'i kendi terminalinizde çalıştırırsınız.

### 1. Veritabanını başlat

```bash
docker compose up -d db
```

Hazır olduğunu doğrulamak için:

```bash
docker compose exec db pg_isready -U miniscore_user
# "accepting connections" görünce devam edin
```

### 2. Backend'i başlat (yeni terminal)

```bash
cd miniscore/backend
./mvnw spring-boot:run
```

> Not: goal `spring-boot:run` — tire ile. `spring:boot-run` yazarsanız
> "No plugin found for prefix 'spring'" hatası alırsınız.

Backend: http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui.html

### 3. Frontend'i başlat (yeni terminal)

```bash
cd miniscore/frontend
npm install       # sadece ilk sefer
npm run dev
```

Frontend: http://localhost:5173

> Backend CORS yalnızca `http://localhost:5173` (Vite) ve `http://localhost:3000`
> (Docker/Nginx) origin'lerine izin verir; frontend bu portlarda çalışmalıdır.

---

## Giriş bilgileri

Uygulama ilk açıldığında bir yönetici hesabı otomatik oluşturulur (AdminSeeder):

| Alan | Değer |
|------|-------|
| E-posta | `admin@findeks.com` |
| Şifre | `admin12345` |

Yeni kayıtlar `USER` rolüyle oluşur; `ADMIN` yetkisini yalnızca mevcut bir admin
(yönetim panelinden) verebilir.

---

## E-posta ile 2 adımlı doğrulama (2FA) — Gmail kurulumu

2FA **varsayılan olarak kapalıdır**; açmadan uygulama sorunsuz çalışır. Açmak için
bir Gmail hesabı + **Uygulama Şifresi** (App Password) gerekir (normal Gmail şifresi çalışmaz).

1. Google hesabında **2-Step Verification**'ı aç.
2. Google Hesabı → Security → **App passwords** → yeni bir uygulama şifresi üret (16 hane).
3. Bu değerleri ortam değişkeni olarak ver (backend'i başlatmadan **önce**):

   ```bash
   # Git Bash / Linux / macOS
   export MAIL_USERNAME="senin.adresin@gmail.com"
   export MAIL_PASSWORD="16haneliuygulamasifresi"
   ```
   ```powershell
   # PowerShell
   $env:MAIL_USERNAME = "senin.adresin@gmail.com"
   $env:MAIL_PASSWORD = "16haneliuygulamasifresi"
   ```
   Docker'da: aynı değişkenleri host'ta ayarla; `docker compose` bunları backend'e geçirir.

4. Backend'i başlat, admin olarak giriş yap → **Yönetim → Sistem Ayarları** →
   "E-posta ile 2 adımlı doğrulama"yı **Aç**.

> **Not:** Yöneticiler 2FA'dan muaftır (seedlenen `admin@findeks.com` gerçek bir kutu değildir),
> böylece 2FA açıkken bile panele girip kapatabilirsiniz. Test ederken kapatmak için
> aynı yerden **Kapat** deyin.

## Faydalı komutlar

```bash
# Backend testleri
cd miniscore/backend && ./mvnw test

# Frontend prod derlemesi
cd miniscore/frontend && npm run build

# Sadece DB loglarını izle
docker compose logs -f db
```
