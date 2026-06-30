package com.findeks.miniscore.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credit_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(nullable = false)
   private Integer score;             // 0 – 1900 aralığı

   @Column(nullable = false)
   private String riskCategory;       // DUSUK_RISK / ORTA_RISK / YUKSEK_RISK

   @CreationTimestamp
   private LocalDateTime queriedAt;
}