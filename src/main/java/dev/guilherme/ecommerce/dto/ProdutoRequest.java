package dev.guilherme.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(min = 2, max = 140)
        String nome,

        @Size(max = 1000, message = "descricao deve ter no máximo 1000 caracteres")
        String descricao,

        @NotNull(message = "preco é obrigatório")
        @DecimalMin(value = "0.01", message = "preco deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "preco inválido (máx. 2 casas decimais)")
        BigDecimal preco,

        @NotNull(message = "estoque é obrigatório")
        @PositiveOrZero(message = "estoque não pode ser negativo")
        Integer estoque
) {
}
