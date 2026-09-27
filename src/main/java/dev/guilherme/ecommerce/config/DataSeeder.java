package dev.guilherme.ecommerce.config;

import dev.guilherme.ecommerce.domain.Produto;
import dev.guilherme.ecommerce.domain.Role;
import dev.guilherme.ecommerce.domain.Usuario;
import dev.guilherme.ecommerce.repository.ProdutoRepository;
import dev.guilherme.ecommerce.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Semeia um admin e produtos de exemplo no ambiente de desenvolvimento (não em testes). */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final ProdutoRepository produtos;
    private final PasswordEncoder encoder;

    public DataSeeder(UsuarioRepository usuarios, ProdutoRepository produtos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.produtos = produtos;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() == 0) {
            usuarios.save(new Usuario("Admin", "admin@loja.dev", encoder.encode("admin12345"), Role.ADMIN));
        }
        if (produtos.count() == 0) {
            produtos.save(new Produto("Camiseta básica", "Algodão penteado", new BigDecimal("59.90"), 40));
            produtos.save(new Produto("Caneca dev", "Cerâmica 300ml", new BigDecimal("39.90"), 100));
            produtos.save(new Produto("Teclado mecânico", "Switch marrom, ABNT2", new BigDecimal("349.00"), 15));
        }
    }
}
