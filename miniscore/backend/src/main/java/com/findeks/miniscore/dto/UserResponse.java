package com.findeks.miniscore.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Admin'in gordugu kullanici gorunumu.
 *
 * NEDEN User entity'sini dogrudan donmuyoruz? DTO deseninin butun sebebi burada:
 *
 * 1) GUVENLIK: User'da "password" alani var (BCrypt hash'i olsa bile). Entity'yi
 *    donsek Jackson onu JSON'a yazardi -> tum hash'ler API'den sizardi.
 * 2) LAZY LOADING: User.creditScores @OneToMany(LAZY). Jackson serialize ederken
 *    o listeye dokunur, Hibernate transaction disinda kaldigi icin
 *    LazyInitializationException patlar (ya da her kullanici icin ekstra SQL atar).
 * 3) SOZLESME: Entity DB'nin sekli, DTO API'nin sekli. Yarin kolon adini
 *    degistirirsem API'yi kullanan frontend kirilmasin diye ikisi ayri durur.
 *
 * "Tek yonlu esleme": User -> UserResponse cevirisi VAR, tersi YOK.
 * Disaridan gelen veri asla dogrudan entity'ye donusmez (o is RegisterRequest'in).
 */
@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String tcNo;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    // password YOK, creditScores YOK -> kasten.
}
