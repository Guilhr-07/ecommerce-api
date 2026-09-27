package dev.guilherme.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private int estoque;

    /** Caminho relativo da imagem enviada (ex.: /api/produtos/imagens/uuid.png). */
    @Column(length = 300)
    private String imagemUrl;

    protected Produto() {
        // exigido pelo JPA
    }

    public Produto(String nome, String descricao, BigDecimal preco, int estoque) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, int estoque) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
    }

    public void definirImagem(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }
}
