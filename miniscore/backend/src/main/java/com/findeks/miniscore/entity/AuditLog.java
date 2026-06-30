package com.findeks.miniscore.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
 
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false)
   private String action;             // SCORE_QUERY | LOGIN | REGISTER
 
   @Column(nullable = false)
   private String performedBy;        // İşlemi gerçekleştiren kullanıcının e-postası
 
   private String details;            // Ek bilgi alanı
 
   @CreationTimestamp
   private LocalDateTime createdAt;
}