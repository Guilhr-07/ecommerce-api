package dev.guilherme.ecommerce.dto;

import dev.guilherme.ecommerce.domain.Produto;
import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        int estoque,
        String imagemUrl
) {
    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(),
                p.getPreco(), p.getEstoque(), p.getImagemUrl());
    }
}
