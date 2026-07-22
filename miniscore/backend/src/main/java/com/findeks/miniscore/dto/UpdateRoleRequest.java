package com.findeks.miniscore.dto;

import com.findeks.miniscore.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * PUT /api/admin/users/{id}/role isteginin govdesi.
 *
 * Tipi String degil Role (enum) yaptik: Jackson gelen "ADMIN" metnini enum'a cevirir,
 * "MUDUR" gibi tanimsiz bir deger gelirse daha controller'a girmeden reddedilir.
 * Yani gecersiz rol ihtimalini tip sistemine yaptirdik, kendi if'imizle degil.
 *
 * @NotNull: alan hic gonderilmezse (null) @Valid bunu yakalar -> 400.
 */
@Data
public class UpdateRoleRequest {

    @NotNull(message = "Rol boş olamaz")
    private Role role;

    // Hassas islem: admin bu isteği yaparken kendi sifresini TEKRAR girer (re-authentication).
    // Boylece acik kalmis bir oturumu ele geciren biri, sifreyi bilmeden rol yukseltemez.
    @NotBlank(message = "Şifre boş olamaz")
    private String password;
}
