package com.findeks.miniscore.dto;

import com.findeks.miniscore.entity.Role;

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
}
