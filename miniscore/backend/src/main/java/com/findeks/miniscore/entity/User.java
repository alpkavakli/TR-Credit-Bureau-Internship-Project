package com.findeks.miniscore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false)
   private String firstName;

   @Column(nullable = false)
   private String lastName;

   @Column(unique = true, nullable = false, length = 11)
   private String tcNo;               // TC kimlik numarası

   @Column(unique = true, nullable = false)
   private String email;

   @Column(nullable = false)
   private String password;           // BCrypt ile şifrelenmiş olarak saklanır

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private Role role = Role.USER;

   @CreationTimestamp
   private LocalDateTime createdAt;

   @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
   private List<CreditScore> creditScores = new ArrayList<>();
   
}
