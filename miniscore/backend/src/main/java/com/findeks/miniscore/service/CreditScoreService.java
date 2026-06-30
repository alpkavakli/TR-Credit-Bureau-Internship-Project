package com.findeks.miniscore.service;

import org.springframework.stereotype.Service;

import com.findeks.miniscore.dto.CreditScoreResponse;
import com.findeks.miniscore.entity.AuditLog;
import com.findeks.miniscore.entity.CreditScore;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.repository.AuditLogRepository;
import com.findeks.miniscore.repository.CreditScoreRepository;
import com.findeks.miniscore.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class CreditScoreService {
 
   private final CreditScoreRepository creditScoreRepository;
   private final UserRepository userRepository;
   private final AuditLogRepository auditLogRepository;
 
   public CreditScoreResponse queryScore(String email) {
       User user = userRepository.findByEmail(email).orElseThrow();
 
       int score        = calculateMockScore(user.getTcNo());
       String risk      = categorize(score);
 
       // Skor sorgusunu kaydet
      creditScoreRepository.save(CreditScore.builder()
              .user(user).score(score).riskCategory(risk).build());
 
       // Denetim günlüğüne yaz
      auditLogRepository.save(AuditLog.builder()
              .action("SCORE_QUERY")
               .performedBy(email)
               .details("Skor: " + score)
               .build());
 
       return new CreditScoreResponse(score, risk,
               user.getFirstName(), user.getLastName());
   }
 
   // TC rakamlarının toplamı 1900 aralığına eşlenir
   private int calculateMockScore(String tcNo) {
       int sum = tcNo.chars().map(c -> c - '0').sum();
       return Math.abs((sum * 1900) % 1900);
   }
 
   private String categorize(int score) {
       if (score >= 1300) return "DUSUK_RISK";
       if (score >= 900)  return "ORTA_RISK";
       return "YUKSEK_RISK";
   }
}