# E-commerce API

API REST de catálogo de loja: cadastro e login com JWT, produtos com busca por nome e paginação, upload de imagem do produto e documentação Swagger. Java 21, Spring Boot 4.1, Spring Security.

## Por que existe

Terceiro projeto da minha trilha de backend (Mês 4), depois do Gestor de Tarefas e do Controle Financeiro. Aqui o assunto é a borda da API: quem pode escrever, como o token é emitido e conferido, o que acontece com o arquivo que o usuário manda, e o que a API responde quando algo dá errado.

## Como funciona

1. `POST /api/auth/register` cria o usuário (senha em BCrypt) e devolve um JWT HS256 válido por 1 hora.
2. O cliente manda `Authorization: Bearer <token>` para criar, editar e apagar produtos. Leitura é pública.
3. `POST /api/produtos/{id}/imagem` recebe PNG, JPEG ou WebP de até 5 MB, grava com nome UUID e devolve a URL.
4. Swagger UI em `/swagger-ui.html`, com botão Authorize para colar o token.

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@loja.dev","senha":"senha12345"}' | jq -r .token)

curl -X POST localhost:8080/api/produtos \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nome":"Caneca","preco":39.90,"estoque":100}'

curl -X POST localhost:8080/api/produtos/1/imagem \
  -H "Authorization: Bearer $TOKEN" -F 'arquivo=@foto.png'
```

## Arquitetura

```
cliente HTTP -> JwtAuthenticationFilter -> SecurityFilterChain -> controllers -> services -> repositories -> H2 ou PostgreSQL
                                                                                  \-> ArmazenamentoService -> disco (UPLOAD_DIR)
```

- `security/`: `JwtService` emite e valida; o filtro lê o header e preenche o `SecurityContext`; `SecurityConfig` define o que é público.
- `service/ArmazenamentoService`: único lugar que toca o disco.
- `config/DataSeeder`: admin e 3 produtos de exemplo, só no dev com H2.

## Decisões técnicas

| Decisão | Por quê |
| --- | --- |
| Sessão `STATELESS` e JWT no header | Sem sessão no servidor, sem cookie, então CSRF desligado faz sentido |
| `jjwt-gson` no lugar de `jjwt-jackson` | O Boot 4 usa Jackson 3; o `jjwt-jackson` puxa Jackson 2 e conflita |
| `JWT_SECRET` sem valor padrão no perfil `postgres` | Sem a variável a API não sobe, em vez de assinar token com a chave de exemplo publicada aqui |
| Arquivo salvo com nome UUID e extensão vinda de lista fechada | O nome do usuário nunca vira caminho; leitura confere `startsWith(raiz)` depois de `normalize()` |
| `/error` liberado no `SecurityConfig` | Com a rota protegida, todo 500 chegava ao cliente como 401 e escondia a causa |
| Senha de 8 a 72 caracteres | O BCrypt recusa mais de 72 bytes com exceção; antes disso virava 500 |
| Flyway no perfil `postgres`, `ddl-auto=validate` | Schema com histórico em banco de verdade; `CHECK (preco > 0)` e `CHECK (estoque >= 0)` no banco |

## Rodando localmente

Pré-requisito: JDK 21 ou mais novo.

```bash
./mvnw spring-boot:run
```

H2 em memória com um admin (`admin@loja.dev` / `admin12345`) e 3 produtos. Swagger em `http://localhost:8080/swagger-ui.html`.

Com PostgreSQL no Docker:

```bash
cp .env.example .env            # e troque o JWT_SECRET (openssl rand -base64 48)
docker compose up --build
```

No perfil `postgres` não existe admin semeado nem produto de exemplo: registre um usuário e crie os produtos.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `JWT_SECRET` | chave de exemplo no dev; obrigatória no perfil `postgres` | assinatura HS256, 32 caracteres ou mais |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | H2 em memória | PostgreSQL |
| `UPLOAD_DIR` | `uploads` | pasta das imagens (ignorada pelo git) |
| `SPRING_PROFILES_ACTIVE` | nenhum | `postgres` liga Flyway e validação do schema |

## Endpoints

9 rotas.

| Método | Rota | Acesso | Resposta |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | público | 201 + token, 400, 409 (email em uso) |
| `POST` | `/api/auth/login` | público | 200 + token, 401 |
| `GET` | `/api/produtos?nome=&page=&size=&sort=` | público | 200 (página), 400 |
| `GET` | `/api/produtos/{id}` | público | 200, 404 |
| `GET` | `/api/produtos/imagens/{arquivo}` | público | 200, 404 |
| `POST` | `/api/produtos` | token | 201, 400, 401 |
| `PUT` | `/api/produtos/{id}` | token | 200, 400, 401, 404 |
| `DELETE` | `/api/produtos/{id}` | token | 204, 401, 404 |
| `POST` | `/api/produtos/{id}/imagem` | token | 200, 401, 404, 409 (tipo não aceito), 413 |

## Testes

```bash
./mvnw verify
```

10 testes de integração (`@SpringBootTest` + MockMvc, H2, perfil `test` com segredo JWT próprio e seeder desligado):

- `EcommerceApiIntegrationTest` (8): registro e escrita protegida, login, email duplicado, senha errada, senha curta, senha acima de 72, `sort` inválido e `/error` público.
- `MigracaoFlywayTest` (1): aplica as migrations num H2 em modo PostgreSQL com `ddl-auto=validate`.
- `EcommerceApiApplicationTests` (1): o contexto sobe.

O upload não tem teste automatizado; foi conferido à mão contra o PostgreSQL (upload 200, download 200 com `image/png`, `..%2F..%2Fpom.xml` recusado com 400).

## O que ficou de fora

- Autorização por papel. O enum `Role` tem `ADMIN`, mas qualquer usuário registrado cria, edita e apaga produto. É o primeiro ajuste que eu faria: `hasRole("ADMIN")` nas rotas de escrita e um jeito seguro de criar o primeiro admin em produção.
- O tipo da imagem é conferido pelo `Content-Type` que o cliente declara, não pelos bytes do arquivo.
- Trocar a imagem de um produto deixa a antiga no disco, e apagar o produto também.
- O limite de 72 da senha é em caracteres. Uma senha com muitos acentos passa de 72 bytes e ainda dá 500.
- Sem rate limit no login, sem refresh token e sem forma de revogar um token antes de expirar.
- Não há carrinho, pedido nem pagamento: é um catálogo.

## Aprendizados

- Em Spring Security, erro interno é encaminhado para `/error`, e essa rota passa pelos mesmos filtros. Protegida, ela transforma bug do servidor em "não autenticado".
- Segredo com valor padrão no `application.properties` sobe em produção sem ninguém perceber. Tirar o padrão só do perfil de produção mantém o dev funcionando com `./mvnw spring-boot:run`.

## Licença

MIT, veja [LICENSE](LICENSE).
