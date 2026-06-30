package com.findeks.miniscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.AuditLog;
import com.findeks.miniscore.entity.User;



public interface AuditLogRepository extends JpaRepository<User, Long>{

    public void save(AuditLog build);
    
    
}
