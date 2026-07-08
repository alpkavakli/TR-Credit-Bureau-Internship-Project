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
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails { //spring anlasın diye userdetails koyduk
//https://www.geeksforgeeks.org/advance-java/spring-security-userdetailsservice-and-userdetails-with-example/  
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
   } /*Spring security'de authority yetki kavramı var, kullanıcının yapabildiği her şey bir yetki ile temsil ediliyor, en yaygın türü roller (user, admin, etc).
    Spring bu yetkileri GrantedAuthority diye bi interface ile temsil ediyo ve o sadece String getAuthority() metodunu barındırır.
    SimpleGrantedAuthority de bize Spring tarafından hazır verilen bir class.
    Bir de Security Config'de .hasRole("admin")vardı onu karşılasın diye "ROLE_ADMIN" isimli yetki yapmalıyız.
      */
    @Override
    public String getUsername() {
      return email; 
      //AuthService.login'de kullanıcı getEmail() ve getPassword() ile giriş yapıyor
      //sonra bize customer login srevice gerekecek loadUserByUsername yazacağız, claude dedi
      //JwtService'te de userDetails.getUsername()'i kullanıyoruz
   }
   
}
