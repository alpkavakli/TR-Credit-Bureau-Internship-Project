package com.findeks.miniscore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;   // hazir CRUD (save, findById, findAll...) saglar

import com.findeks.miniscore.entity.CreditScore;
import com.findeks.miniscore.entity.User;

// JpaRepository<Entity, IdTipi>: Spring Data JPA method ISMINDEN otomatik SQL uretir, govde yazmayiz.
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

    // Bir kullanicinin EN GUNCEL snapshot'i = en yuksek weekIndex'li satir.
    // "findTop...OrderByWeekIndexDesc" -> SELECT ... WHERE user=? ORDER BY week_index DESC LIMIT 1
    // Optional: satir olmayabilir (hic sorgu yapmamis kullanici) -> null yerine bos Optional.
    Optional<CreditScore> findTopByUserOrderByWeekIndexDesc(User user);

    // Tum gecmis, eskiden yeniye (zaman cizelgesi) -> ORDER BY week_index ASC
    List<CreditScore> findByUserOrderByWeekIndexAsc(User user);
}
