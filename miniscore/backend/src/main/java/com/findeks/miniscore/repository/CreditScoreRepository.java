package com.findeks.miniscore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.CreditScore;

public interface CreditScoreRepository extends JpaRepository<CreditScore, Long>{
    List<CreditScore> findByUserEmail(String email);
}
