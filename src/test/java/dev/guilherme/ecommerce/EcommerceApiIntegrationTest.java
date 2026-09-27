package dev.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.guilherme.ecommerce.domain.Role;
import dev.guilherme.ecommerce.domain.Usuario;
import dev.guilherme.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EcommerceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder encoder;

    private String registrar(String nome, String email, String senha) throws Exception {
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    /** Nenhuma rota cria admin: o teste grava direto no banco, como o bootstrap de produção faz. */
    private String tokenAdmin(String email) throws Exception {
        usuarios.save(new Usuario("Admin", email, encoder.encode("senha12345"), Role.ADMIN));
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private Long criarProdutoComoAdmin(String tokenAdmin) throws Exception {
        String json = mockMvc.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static String produtoJson() {
        return "{\"nome\":\"Camiseta\",\"descricao\":\"Algodão\",\"preco\":59.90,\"estoque\":10}";
    }

    @Test
    void anonimoRecebe401EAdminCria() throws Exception {
        // sem token: escrita bloqueada, no mesmo formato de erro do resto da API
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Não autenticado")));

        // admin: cria
        String token = tokenAdmin("admin-cria@loja.dev");
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
        // o token é aceito: o USER passa da autenticação e para na autorização (403, não 401)
        mockMvc.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComumNaoEscreveEmProdutos() throws Exception {
        String tokenUser = registrar("Fábio", "fabio@loja.dev", "senha12345");
        Long id = criarProdutoComoAdmin(tokenAdmin("admin-fabio@loja.dev"));

        mockMvc.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Acesso negado")));

        mockMvc.perform(put("/api/produtos/" + id)
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/produtos/" + id)
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden());

        var png = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/produtos/" + id + "/imagem").file(png)
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden());

        // leitura continua pública e o produto não foi apagado
        mockMvc.perform(get("/api/produtos/" + id)).andExpect(status().isOk());
    }

    @Test
    void registroIgnoraPapelEnviadoPeloCliente() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Gil\",\"email\":\"gil@loja.dev\",\"senha\":\"senha12345\","
                                + "\"role\":\"ADMIN\",\"papel\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        assertThat(usuarios.findByEmailIgnoreCase("gil@loja.dev").orElseThrow().getRole())
                .isEqualTo(Role.USER);
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
