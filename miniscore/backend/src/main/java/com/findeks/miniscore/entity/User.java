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
import jakarta.persistence.PrePersist;   // INSERT oncesi calisan JPA callback'i
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

import com.findeks.miniscore.common.EmailUtils;   // e-posta normalizasyon kurali (tek kaynak)

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

   /*
    * @PrePersist = JPA'nin KENDI lifecycle callback'i.
    * @CreationTimestamp'ten farki:
    *   - @CreationTimestamp Hibernate'e ozel (JPA standardi degil) ve tek is yapar: tarih basar.
    *   - @PrePersist standart JPA'dir ve iceride ISTEDIGIN kodu calistirirsin.
    * Ne zaman calisir: INSERT'ten hemen once. (Kardesi @PreUpdate -> UPDATE'ten hemen once.)
    *
    * Buradaki is: e-postayi kucuk harfe cevirmek. "Alp@Test.com" ile "alp@test.com"
    * ayni kisidir; DB'deki unique kisiti ise buyuk/kucuk harfe duyarlidir, yani
    * normalize etmezsek ayni adresle IKI hesap acilabilirdi.
    * Entity seviyesine koyduk cunku kaydin hangi yoldan geldigi (register, seeder)
    * fark etmeksizin bu kural yeni kayitlarda HER ZAMAN uygulansin istiyoruz.
    *
    * !!! BURADA ONCE @PreUpdate DE VARDI, KALDIRILDI - nedeni onemli bir ders:
    * @PreUpdate entity'nin HER guncellemesinde calisir, guncellemenin e-posta ile
    * ilgisi olmasa bile. Yani admin sadece ROL degistirdiginde bile e-posta sessizce
    * yeniden yaziliyordu. Testte goruldu: PUT /users/4/role -> Ayse@test.com
    * kendiliginden ayse@test.com oldu. Sonuclari:
    *   - alakasiz bir islem kullanicinin GIRIS KIMLIGINI degistiriyordu,
    *   - elindeki JWT'nin sub'i eski e-posta kaliyordu -> JwtAuthFilter kullaniciyi
    *     bulamiyor -> 401 yerine 500,
    *   - DB'de hem "X@a.com" hem "x@a.com" olsaydi unique kisiti patlardi.
    * Ders: lifecycle callback'i "her ihtimale karsi" genis tutmak bedava degil;
    * yazdigi veri, o guncellemeyi isteyen kodun HABERI OLMADAN degisiyor.
    * Mevcut kayitlarin e-postasini degistiren bir akis zaten yok; olursa
    * normalizasyonu o akisin kendisi EmailUtils.normalize() ile yapmali.
    */
   @PrePersist
   private void normalizeEmail() {
      email = EmailUtils.normalize(email);   // kural tek yerde: EmailUtils
   }
}
