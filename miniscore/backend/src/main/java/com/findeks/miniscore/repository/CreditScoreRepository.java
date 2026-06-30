package com.findeks.miniscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.CreditScore;
import com.findeks.miniscore.entity.User;

public interface CreditScoreRepository extends JpaRepository<User, Long>{

    public void save(CreditScore build);
    
}
