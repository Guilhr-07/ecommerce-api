package dev.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EcommerceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String registrar(String nome, String email, String senha) throws Exception {
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String produtoJson() {
        return "{\"nome\":\"Camiseta\",\"descricao\":\"Algodão\",\"preco\":59.90,\"estoque\":10}";
    }

    @Test
    void registraLogaEProtegeEscrita() throws Exception {
        String token = registrar("Ana", "ana@loja.dev", "senha12345");

        // sem token: escrita bloqueada
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isUnauthorized());

        // com token: cria
        mockMvc.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome", is("Camiseta")));

        // leitura é pública e paginada
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    void loginRetornaTokenValido() throws Exception {
        registrar("Bruno", "bruno@loja.dev", "senha12345");
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bruno@loja.dev\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(json, "$.token");
        // token funciona numa rota protegida
        mockMvc.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        registrar("Carla", "carla@loja.dev", "senha12345");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carla 2\",\"email\":\"carla@loja.dev\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Regra de negócio violada")));
    }

    @Test
    void rejeitaLoginComSenhaErrada() throws Exception {
        registrar("Diego", "diego@loja.dev", "senha12345");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"diego@loja.dev\",\"senha\":\"errada9999\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaSenhaCurtaNoRegistro() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Eva\",\"email\":\"eva@loja.dev\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void rejeitaSenhaAcimaDoLimiteDoBcrypt() throws Exception {
        // BCrypt só aceita até 72 bytes; acima disso o encoder lança exceção (era 500).
        String senhaLonga = "a".repeat(80);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Leo\",\"email\":\"leo@loja.dev\",\"senha\":\"" + senhaLonga + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void ordenacaoPorCampoInexistenteRetorna400() throws Exception {
        mockMvc.perform(get("/api/produtos").param("sort", "naoExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Parâmetro inválido")));
    }

    @Test
    void rotaDeErroEhPublica() throws Exception {
        // Erro em rota pública é encaminhado para /error; se /error exigir token,
        // qualquer 500 chega ao cliente como 401 e esconde o problema real.
        mockMvc.perform(get("/error"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}
