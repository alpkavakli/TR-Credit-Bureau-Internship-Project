package com.findeks.miniscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.AuditLog;



public interface AuditLogRepository extends JpaRepository<AuditLog, Long>{

    
    
    
}
