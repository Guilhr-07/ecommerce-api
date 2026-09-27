package dev.guilherme.ecommerce.config;

import dev.guilherme.ecommerce.domain.Role;
import dev.guilherme.ecommerce.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primeiro admin em produção. Se ADMIN_EMAIL estiver definido e esse usuário já existir
 * como USER, ele é promovido a ADMIN na subida. Nunca cria usuário nem senha a partir de
 * variável de ambiente: a conta é registrada antes pelo /api/auth/register.
 */
@Component
@Profile("postgres")
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarios;
    private final String adminEmail;

    public AdminBootstrap(UsuarioRepository usuarios, @Value("${app.admin.email:}") String adminEmail) {
        this.usuarios = usuarios;
        this.adminEmail = adminEmail;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        usuarios.findByEmailIgnoreCase(adminEmail.strip()).ifPresentOrElse(usuario -> {
            if (usuario.getRole() == Role.ADMIN) {
                return;
            }
            usuario.promoverAAdmin();
            usuarios.save(usuario);
            log.info("ADMIN_EMAIL: usuário id={} promovido a ADMIN", usuario.getId());
        }, () -> log.warn("ADMIN_EMAIL definido, mas nenhum usuário com esse email existe; "
                + "registre a conta e reinicie a API"));
    }
}
