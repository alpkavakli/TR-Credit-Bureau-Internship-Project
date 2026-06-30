package com.findeks.miniscore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    public boolean existsByEmail(String email);

    public Optional<User> findByEmail(String email); //Optional olmalıymış niye idk

}
