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
        // BCrypt aceita até 72 bytes. O limite aqui é em caracteres: senha longa com acento ainda pode passar disso.
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha
) {
}
