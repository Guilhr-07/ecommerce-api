package dev.guilherme.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(min = 2, max = 80)
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, message = "senha deve ter no mínimo 8 caracteres")
        // BCrypt aceita até 72 bytes; o limite é em bytes porque letra acentuada ocupa 2
        @MaxBytesUtf8(value = 72, message = "senha deve ter no máximo 72 bytes (letra acentuada conta 2)")
        String senha
) {
}
