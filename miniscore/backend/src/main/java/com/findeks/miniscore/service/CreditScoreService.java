package com.findeks.miniscore.service;

import java.time.LocalDate;         // gun bazli tarih (saat yok) -> hafta hesabi icin
import java.time.LocalDateTime;     // user.getCreatedAt() bu tipte
import java.util.List;
import java.util.stream.Collectors; // stream().collect(...) icin

import org.springframework.stereotype.Service;                       // bu class bir "service" bean'i (@Component'in kuzeni)
import org.springframework.transaction.annotation.Transactional;     // metodu tek DB transaction'ina sarar (atomic)

import com.findeks.miniscore.dto.CreditScoreResponse;
import com.findeks.miniscore.entity.CreditScore;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.NotFoundException;
import com.findeks.miniscore.repository.CreditScoreRepository;
import com.findeks.miniscore.repository.UserRepository;

import lombok.RequiredArgsConstructor;   // final alanlar icin constructor uretir -> Spring bunlari inject eder (DI)

@Service
@RequiredArgsConstructor
public class CreditScoreService {

   // --- sabitler: "sihirli sayilari" isimlendir, tek yerden ayarla ---
   private static final int MIN_SCORE = 1,   MAX_SCORE = 1900;   // Findeks araligi -> clamp siniri
   private static final int BASE_MIN  = 600, BASE_MAX  = 1400;   // ID'den turetilen baslangic skoru bandi
   private static final int MIN_VOL   = 50,  MAX_VOL   = 500;    // kisiye ozel haftalik oynaklik siniri (gurultu genligi)
   private static final double REVERSION = 0.35;                 // base'e geri cekim gucu (0=saf yuruyus, 1=aninda base'e yapisir)
   private static final int LOW_RISK_THRESHOLD = 1300;           // >= -> DUSUK_RISK
   private static final int MID_RISK_THRESHOLD = 900;            // >= -> ORTA_RISK

   // final + @RequiredArgsConstructor -> Spring bu bean'leri constructor'dan enjekte eder
   private final CreditScoreRepository creditScoreRepository;
   private final UserRepository userRepository;
   // AuditLogRepository yerine artik AuditService: repository'ye dogrudan degil,
   // ortak servis uzerinden yaziyoruz (audit kurallari tek yerde).
   private final AuditService auditService;

   // @Transactional: icerideki TUM DB yazmalari tek transaction. Ya hepsi commit olur ya hicbiri (atomic).
   // Burada is: eksik haftalarin snapshot'larini uret + audit log yaz -> hepsi birlikte.
   @Transactional
   public CreditScoreResponse queryScore(String email) {
              User user = userRepository.findByEmail(email)
               .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: " + email));


       generateMissingWeeks(user, currentWeek());   // gecmis + bu haftayi (yoksa) uret, append-only

       // en guncel skor = en yuksek weekIndex'li snapshot (SUM degil, LATEST)
       CreditScore latest = creditScoreRepository
               .findTopByUserOrderByWeekIndexDesc(user).orElseThrow();

       // her sorgu bir denetim olayidir -> her zaman logla.
       // Eskiden AuditLog.builder() burada elle kuruluyordu; ayni kod AuthService'e de
       // gerekince tek yere (AuditService) tasidik -> tekrar yok, degisirse tek dosya degisir.
       auditService.log("SCORE_QUERY", email, "Skor: " + latest.getScore());

       return toResponse(user, latest);
   }

   // readOnly = true: sadece okuma -> Hibernate yazma kontrollerini atlar, biraz daha hizli/guvenli niyet bildirimi
   @Transactional(readOnly = true)
   public List<CreditScoreResponse> getHistory(String email) {
             User user = userRepository.findByEmail(email)
               .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: " + email));
       return creditScoreRepository.findByUserOrderByWeekIndexAsc(user).stream()
               .map(cs -> toResponse(user, cs))
               .collect(Collectors.toList());
   }

   // ---------------------------------------------------------------
   //  Gercek bir kredi burosunun yerine gecen MOCK uretici.
   //  Not: normalde bunu haftalik ZAMANLANMIS bir job yapar; biz sorgu aninda
   //  tembel (lazy) uretiyoruz. Skor "hesaplanan" degil "saklanan" veri gibi davranir.
   // ---------------------------------------------------------------
   private void generateMissingWeeks(User user, long currentWeek) {
       CreditScore latest = creditScoreRepository
               .findTopByUserOrderByWeekIndexDesc(user).orElse(null);

       long startWeek;
       int  prevScore;
       if (latest == null) {                          // hic snapshot yok -> ilkini olustur
           startWeek = weekOf(user.getCreatedAt());   // hesabin acildigi hafta
           prevScore = clamp(baseScore(user));        // baslangic = ID'den turetilen base
           saveSnapshot(user, startWeek, prevScore);
           startWeek++;
       } else {                                       // en son haftadan devam et
           startWeek = latest.getWeekIndex() + 1;
           prevScore = latest.getScore();
       }

       // MEAN-REVERTING WALK: her hafta = onceki + base'e cekim + gurultu, sonra 1..1900'e clamp.
       // Snapshot MUTLAK skoru saklar; delta/gurultu sadece bir sonrakini URETMEK icin kullanilir (saklanmaz).
       for (long w = startWeek; w <= currentWeek; w++) {
           int newScore = nextScore(user, prevScore, w);
           saveSnapshot(user, w, newScore);
           prevScore = newScore;
       }
   }

   private void saveSnapshot(User user, long week, int score) {
       creditScoreRepository.save(CreditScore.builder()
               .user(user)
               .weekIndex(week)
               .score(score)
               .riskCategory(categorize(score))
               .build());
   }

   // ---------------------------------------------------------------
   //  Deterministik uretici fonksiyonlar: hepsi ID (tcNo) + bir "salt"tan turer.
   //  Ayni girdi -> ayni cikti (deterministik). Gercek veri olsaydi bunlarin yerini
   //  gercek finansal veri alirdi; sekli aynen boyle kalirdi.
   // ---------------------------------------------------------------

   // Baslangic skoru: ID'ye gore 600..1400 bandinda sabit bir sayi
   private int baseScore(User user) {
       return BASE_MIN + Math.floorMod(seed(user, 0, "BASE"), BASE_MAX - BASE_MIN + 1);
   }

   // Kisiye ozel haftalik oynaklik: ID'ye gore 50..500 arasi (min 50, max 500)
   private int volatility(User user) {
       return MIN_VOL + Math.floorMod(seed(user, 0, "VOL"), MAX_VOL - MIN_VOL + 1);
   }

   // O haftaya ozel gurultu (rastgele bilesen): -vol..+vol arasi, (ID, hafta) ciftine gore deterministik
   private int weeklyDelta(User user, long week) {
       int vol = volatility(user);
       return Math.floorMod(seed(user, week, "NOISE"), 2 * vol + 1) - vol;
   }

   // Bir sonraki haftanin skoru = onceki + base'e cekim (mean reversion) + gurultu, sonra clamp.
   //  - current base'in USTUNDEyse (yuksek) -> (base-current) negatif -> asagi cekim -> kazanmak zorlasir
   //  - current base'in ALTINDAysa (dusuk)  -> (base-current) pozitif -> yukari cekim -> kaybetmek zorlasir
   //  - ne kadar uzaklasirsan cekim o kadar guclu -> 1900/1 uclari nadir ve gecici olur (yine de mumkun)
   private int nextScore(User user, int current, long week) {
       int base = baseScore(user);
       int pull = (int) Math.round(REVERSION * (base - current));
       int noise = weeklyDelta(user, week);
       return clamp(current + pull + noise);
   }

   // Deterministik, IYI KARISTIRAN hash (splitmix64-tarzi finalizer).
   // Onemli: Objects.hash ardisik girdilerde ("W2886","W2887") neredeyse ardisik ciktilar veriyordu
   // -> gurultu haftadan haftaya ~+1 artiyor, "rastgele yuruyus" duz bir rampaya donuyordu.
   // Buradaki carpim + xor-shift zinciri ardisik haftalari bile birbirinden bagimsiz dagitir.
   private int seed(User user, long week, String salt) {
       long h = user.getTcNo().hashCode() * 0x9E3779B97F4A7C15L
              + week                      * 0xC2B2AE3D27D4EB4FL
              + salt.hashCode()           * 0x165667B19E3779F9L;
       h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL;
       h ^= (h >>> 33); h *= 0xc4ceb9fe1a85ec53L;
       h ^= (h >>> 33);
       return (int) h;
   }

   // ---------------------------------------------------------------
   //  Yardimcilar
   // ---------------------------------------------------------------

   // Bugunku hafta indeksi. toEpochDay = 1970'ten bugune gun sayisi; /7 -> 7 gunluk pencere.
   private long currentWeek() {
       return LocalDate.now().toEpochDay() / 7;
   }

   private long weekOf(LocalDateTime t) {
       return t.toLocalDate().toEpochDay() / 7;
   }

   // Math.floorMod negatiflerde de dogru (0..n-1) sonuc verir; % operatoru negatif verebilir.
   // clamp: skoru [MIN_SCORE, MAX_SCORE] araligina hapseder -> asla 1'in altina / 1900'un ustune cikmaz.
   private int clamp(int score) {
       return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
   }

   private String categorize(int score) {
       if (score >= LOW_RISK_THRESHOLD) return "DUSUK_RISK";
       if (score >= MID_RISK_THRESHOLD) return "ORTA_RISK";
       return "YUKSEK_RISK";
   }

   // Entity -> DTO. Haftanin gercek tarihi weekIndex'ten turetilir (queriedAt'ten degil).
   private CreditScoreResponse toResponse(User user, CreditScore cs) {
       LocalDate weekStart = LocalDate.ofEpochDay(cs.getWeekIndex() * 7);
       return new CreditScoreResponse(
               cs.getScore(),
               cs.getRiskCategory(),
               user.getFirstName(),
               user.getLastName(),
               weekStart);
   }
}
