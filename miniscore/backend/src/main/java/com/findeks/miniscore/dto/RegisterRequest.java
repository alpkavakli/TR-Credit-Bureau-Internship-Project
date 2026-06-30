package com.findeks.miniscore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Size(min = 11, max = 11) private String tcNo;
    @Email   private String email;
    @Size(min = 8) private String password;
}