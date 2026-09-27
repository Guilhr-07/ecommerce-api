# Manual completo — API de E-commerce (com JWT)

> Antes: **[Fundamentos](../MANUAIS/01-FUNDAMENTOS-SPRING-BOOT.md)** (seção 7 é sobre JWT)
> e os manuais dos projetos 1 e 2. Aqui foco no que é **novo e mais avançado**: segurança
> com **Spring Security + JWT**, **upload de imagens** e **documentação Swagger**.

Projeto: **API de catálogo de e-commerce**. Qualquer um pode ver produtos; só quem está
logado cria/edita/apaga. Marco do **Mês 4**.

---

## 1. Como rodar e testar na prática

```bash
cd ecommerce-api
./mvnw spring-boot:run
```

Já vem com um admin (`admin@loja.dev` / `admin12345`) e 3 produtos. Fluxo completo no
terminal:

```bash
# 1. registrar e capturar o token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@loja.dev","senha":"senha12345"}' | jq -r .token)

# 2. criar produto usando o token
curl -X POST localhost:8080/api/produtos \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nome":"Caneca","preco":39.90,"estoque":100}'

# 3. tentar sem token -> 401
curl -i -X POST localhost:8080/api/produtos -H 'Content-Type: application/json' -d '{}'
```

**Swagger (documentação navegável):** abra **http://localhost:8080/swagger-ui.html** —
tem um botão **Authorize** para colar o token e testar as rotas protegidas pelo navegador.

Testes: `./mvnw test` (6 testes).

---

## 2. Autenticação com JWT — o passo a passo no código

Releia a seção 7 dos Fundamentos para a ideia geral. Aqui, as 4 peças da pasta `security/`:

### 2.1. `JwtService.java` — emite e valida o token

```java
public String gerarToken(Usuario usuario) {
    Instant agora = Instant.now();
    return Jwts.builder()
        .subject(usuario.getEmail())                 // "dono" do token
        .claim("role", usuario.getRole().name())     // dado extra
        .issuedAt(Date.from(agora))
        .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))  // validade
        .signWith(key)                               // assinatura (impede adulteração)
        .compact();
}
```

O `key` vem de um **segredo** (`app.jwt.secret`, ≥ 32 caracteres). Para validar:

```java
public String extrairEmailValido(String token) {
    try {
        return Jwts.parser().verifyWith(key).build()
                   .parseSignedClaims(token).getPayload().getSubject();
    } catch (JwtException | IllegalArgumentException ex) {
        return null;                                 // token inválido/expirado → null
    }
}
```

> **Detalhe técnico (Boot 4):** usei a variante **`jjwt-gson`** de propósito. O Spring
> Boot 4 usa Jackson 3 (`tools.jackson`), e a variante `jjwt-jackson` traria Jackson 2
> junto — conflito. Com gson isso não acontece. *(Está no Jarbas, `ERR-JAVA-007`.)*

### 2.2. `UsuarioDetailsService.java` — como o Spring carrega o usuário

Implementa a interface que o Spring Security espera. Busca por email e devolve um objeto
com email, hash da senha e a autoridade `ROLE_USER`/`ROLE_ADMIN`.

### 2.3. `JwtAuthenticationFilter.java` — o porteiro de cada requisição

```java
protected void doFilterInternal(HttpServletRequest req, ... ) {
    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ") && /* ainda não autenticado */ ) {
        String token = header.substring(7);           // tira o "Bearer "
        String email = jwtService.extrairEmailValido(token);
        if (email != null) {
            UserDetails user = userDetailsService.loadUserByUsername(email);
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);   // "logado" para esta requisição
        }
    }
    filterChain.doFilter(req, res);                    // segue o fluxo
}
```

É um `OncePerRequestFilter` (roda uma vez por requisição). Se o token é válido, ele marca
a requisição como autenticada; se não, deixa passar sem autenticação — e aí o Security
barra nas rotas protegidas.

### 2.4. `SecurityConfig.java` — as regras de quem entra onde

```java
http.csrf(csrf -> csrf.disable())                                     // API stateless não usa CSRF
    .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))     // sem sessão no servidor
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()                  // login/registro: livre
        .requestMatchers(HttpMethod.GET, "/api/produtos/**").permitAll() // ver produtos: livre
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .anyRequest().authenticated())                                // o resto: precisa de token
    .exceptionHandling(ex -> ex.authenticationEntryPoint(
        (r, res, e) -> res.sendError(401)))                           // sem token → 401
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

E o **BCrypt** para senhas (nunca senha em texto):

```java
@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
```

No `AuthService`: registrar faz `encoder.encode(senha)` (gera o hash); logar faz
`encoder.matches(senhaDigitada, hashGuardado)` (compara sem nunca descriptografar).

---

## 3. Upload de imagens — `service/ArmazenamentoService.java`

Salva o arquivo no disco e devolve a URL para servi-lo. Dois cuidados de segurança:

```java
// 1. só aceita imagem de verdade
if (!TIPOS_PERMITIDOS.contains(contentType))     // png, jpeg, webp
    throw new RegraNegocioException("Tipo de imagem não suportado");

// 2. nome aleatório + trava contra "path traversal"
String nomeArquivo = UUID.randomUUID() + extensao;
Path destino = raiz.resolve(nomeArquivo).normalize();
if (!destino.startsWith(raiz))                    // impede salvar fora da pasta (../../)
    throw new RegraNegocioException("Caminho de arquivo inválido");
```

O `UUID` evita colisão de nome e esconde o nome original. A trava `startsWith(raiz)`
impede que um nome malicioso escape da pasta de uploads. No controller:

```java
@PostMapping(path = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ProdutoResponse enviarImagem(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo)
```

`multipart/form-data` é o formato de envio de arquivo; o campo se chama `arquivo`. A
imagem é servida por `GET /api/produtos/imagens/{arquivo}` (rota pública).

---

## 4. Documentação Swagger — `config/OpenApiConfig.java`

A biblioteca **springdoc** lê as anotações e gera a página do Swagger sozinha. O único
código é registrar o "esquema de segurança" para aparecer o botão **Authorize**:

```java
.components(new Components().addSecuritySchemes("bearerAuth",
    new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")));
```

Nos controllers, `@Operation(summary = "...")` e `@Tag(...)` viram a descrição na tela.

> **Detalhe (Boot 4):** springdoc **3.0.3** — a linha 3.x é a compatível com Spring Boot
> 4. A 2.8.x é para Boot 3. *(Jarbas, `ERR-JAVA-008`.)*

---

## 5. Paginação e filtro

```java
@GetMapping   // /api/produtos?nome=caneca&page=0&size=12&sort=preco,asc
public Page<ProdutoResponse> listar(@RequestParam(required = false) String nome,
                                    @PageableDefault(size = 12) Pageable pageable)
```

O `Pageable` faz página/tamanho/ordenação automático; `findByNomeContainingIgnoreCase`
filtra por nome (o Spring gera o `WHERE nome ILIKE %...%`).

---

## 6. Papéis (Role) — o que já está pronto e como ativar

Todo usuário tem um `Role` (USER/ADMIN), virando a autoridade `ROLE_USER`/`ROLE_ADMIN`. As
escritas hoje exigem apenas **estar logado**. Para exigir **admin** numa rota (ex.: só
admin apaga), adicione no `SecurityConfig`:

```java
.requestMatchers(HttpMethod.DELETE, "/api/produtos/**").hasRole("ADMIN")
```

O admin de exemplo (`admin@loja.dev`) é criado pelo `config/DataSeeder.java`, que só roda
fora dos testes (`@Profile("!test")`).

---

## 7. Como mexer

- **Adicionar categoria de produto?** Crie uma entidade `Categoria` e um `@ManyToOne` em
  `Produto` (igual ao projeto Financeiro).
- **Trocar armazenamento de imagem para nuvem (S3)?** Troque só o `ArmazenamentoService`
  — o resto do código não muda (por isso ele é uma classe isolada).
- **Aumentar o limite de upload?** `spring.servlet.multipart.max-file-size` no
  `application.properties`.
- **Token durar mais?** `app.jwt.expiration-seconds` (padrão 3600 = 1h).

---

## 8. Segurança — o que lembrar

| Boa prática no projeto | Por quê |
|------------------------|---------|
| Senha só como hash BCrypt | Se vazar o banco, ninguém tem as senhas |
| Segredo do JWT por variável de ambiente | Não fica no código/GitHub |
| Rota de escrita exige token | Estranho não altera o catálogo |
| Upload valida tipo + trava path traversal | Impede arquivo malicioso e fuga de pasta |
| Erros em ProblemDetail | Não vaza stack trace para o cliente |

Este é o projeto mais completo dos três — junta tudo dos anteriores + segurança.
