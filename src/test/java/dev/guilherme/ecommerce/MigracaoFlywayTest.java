package dev.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Aplica as migrations num H2 em modo PostgreSQL e sobe o contexto com ddl-auto=validate:
 * se uma entidade ganhar coluna sem migration correspondente, este teste quebra no CI.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migracao_v1;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false"
})
class MigracaoFlywayTest {

    @Autowired
    private Flyway flyway;

    @Test
    void migrationsAplicamEBatemComAsEntidades() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
