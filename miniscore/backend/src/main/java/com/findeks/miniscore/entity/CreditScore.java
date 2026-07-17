package com.findeks.miniscore.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;   // Hibernate: satir INSERT edilirken alani otomatik doldurur

import jakarta.persistence.Column;
import jakarta.persistence.Entity;          // Bu class bir DB tablosuna karsilik gelir (JPA)
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint; // (user_id, week_index) ciftinin benzersizligini DB'de zorlar
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
// uniqueConstraints: ayni kullanicinin ayni haftasina iki snapshot yazilmasini DB seviyesinde engeller.
// -> "DB son hakem": iki es zamanli sorgu ayni haftayi eklemeye kalkarsa ikincisini DB reddeder.
@Table(name = "credit_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_index"}))
@Data               // Lombok: getter/setter/toString/equals/hashCode uretir
@Builder            // Lombok: CreditScore.builder()....build() zincirini uretir
@NoArgsConstructor  // Lombok: bos constructor (JPA bunu ister)
@AllArgsConstructor // Lombok: tum alanlari alan constructor (@Builder bunu kullanir)
public class CreditScore {

   @Id                                                  // birincil anahtar
   @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB otomatik artan id verir (BIGSERIAL) = monotonik sira
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)   // cok skor -> tek user. LAZY: user'a erisilene kadar DB'den cekilmez
   @JoinColumn(name = "user_id", nullable = false)   // tabloda user_id foreign key kolonu
   private User user;

   // Hangi haftanin snapshot'i. week = epochDay / 7 (7 gunluk pencere).
   // Bu ayni zamanda haftalar arasi sirayi verir -> "en yuksek week = en guncel".
   @Column(name = "week_index", nullable = false)
   private Long weekIndex;

   @Column(nullable = false)
   private Integer score;             // 1 – 1900 (o haftanin MUTLAK skoru, delta degil)

   @Column(nullable = false)
   private String riskCategory;       // DUSUK_RISK / ORTA_RISK / YUKSEK_RISK

   @CreationTimestamp                 // satir DB'ye yazildigi AN. (Not: gecmis haftalar toplu uretilirse hepsi ~ayni ani gosterir;
   private LocalDateTime queriedAt;   //  haftanin gercek tarihi weekIndex'ten turetilir, queriedAt'ten degil.)
}
