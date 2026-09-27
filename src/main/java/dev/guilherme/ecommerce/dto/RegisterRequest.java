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
        @Size(min = 8, max = 100, message = "senha deve ter no mínimo 8 caracteres")
        String senha
) {
}
