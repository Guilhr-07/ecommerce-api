# E-commerce API

API REST de catálogo de loja: cadastro e login com JWT, produtos com busca por nome e paginação, upload de imagem do produto e documentação Swagger. Java 21, Spring Boot 4.1, Spring Security.

## Por que existe

Terceiro projeto da minha trilha de backend (Mês 4), depois do Gestor de Tarefas e do Controle Financeiro. Aqui o assunto é a borda da API: quem pode escrever, como o token é emitido e conferido, o que acontece com o arquivo que o usuário manda, e o que a API responde quando algo dá errado.

## Como funciona

1. `POST /api/auth/register` cria o usuário, sempre com papel `USER` (senha em BCrypt), e devolve um JWT HS256 válido por 1 hora. Papel mandado no JSON é ignorado.
2. Leitura do catálogo é pública. Criar, editar, apagar produto e enviar imagem exige `Authorization: Bearer <token>` de um usuário `ADMIN`; `USER` recebe 403 e anônimo recebe 401, os dois em ProblemDetail.
3. `POST /api/produtos/{id}/imagem` recebe PNG, JPEG ou WebP de até 5 MB, grava com nome UUID e devolve a URL.
4. Swagger UI em `/swagger-ui.html`, com botão Authorize para colar o token.

```bash
# no dev, o admin de exemplo semeado pelo DataSeeder
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@loja.dev","senha":"admin12345"}' | jq -r .token)

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

- `security/`: `JwtService` emite e valida; o filtro lê o header, carrega o usuário e o papel do banco e preenche o `SecurityContext`; `SecurityConfig` é o único lugar que diz o que é público, o que exige login e o que exige `ADMIN`.
- `config/AdminBootstrap`: no perfil `postgres`, promove a `ADMIN` o usuário cujo email está em `ADMIN_EMAIL`.
- `service/ArmazenamentoService`: único lugar que toca o disco.
- `config/DataSeeder`: admin e 3 produtos de exemplo, só no dev com H2.

## Decisões técnicas

| Decisão | Por quê |
| --- | --- |
| Sessão `STATELESS` e JWT no header | Sem sessão no servidor, sem cookie, então CSRF desligado faz sentido |
| `jjwt-gson` no lugar de `jjwt-jackson` | O Boot 4 usa Jackson 3; o `jjwt-jackson` puxa Jackson 2 e conflita |
| `JWT_SECRET` sem valor padrão no perfil `postgres` | Sem a variável a API não sobe, em vez de assinar token com a chave de exemplo publicada aqui |
| Arquivo salvo com nome UUID e extensão vinda de lista fechada | O nome do usuário nunca vira caminho; leitura confere `startsWith(raiz)` depois de `normalize()` |
| Papel `ADMIN` exigido no `SecurityFilterChain`, não em `@PreAuthorize` | Abro um arquivo e vejo o que está protegido. GET em produtos é público e qualquer outro método exige `ADMIN`, então rota nova de escrita já nasce fechada |
| Papel carregado do banco a cada requisição, não da claim do token | Custa uma consulta por requisição, mas promover ou rebaixar alguém vale na hora; com o papel no token, a mudança só valeria quando o token expirasse |
| Primeiro admin por `ADMIN_EMAIL`, promovendo conta já registrada | Nenhuma senha nasce de variável de ambiente nem fica no repositório, e o registro público nunca cria admin |
| 401 e 403 passam pelo `GlobalExceptionHandler` | O cliente recebe o mesmo ProblemDetail do resto da API em vez de resposta vazia |
| `/error` liberado no `SecurityConfig` | Com a rota protegida, todo 500 chegava ao cliente como 401 e escondia a causa |
| Senha de no mínimo 8 caracteres e no máximo 72 bytes UTF-8 | O BCrypt recusa mais de 72 bytes com exceção. Letra acentuada ocupa 2 bytes, então o limite em caracteres deixava passar e virava 500 |
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

No perfil `postgres` não existe admin semeado nem produto de exemplo. Para ter o primeiro admin:

1. Suba a API e registre sua conta em `POST /api/auth/register` (ela nasce `USER`).
2. Ponha o email dela em `ADMIN_EMAIL` no `.env` e reinicie (`docker compose up -d`).
3. Na subida, o `AdminBootstrap` promove essa conta a `ADMIN`. Se o email não existir, só avisa no log. Nenhuma conta nem senha é criada a partir da variável.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `JWT_SECRET` | chave de exemplo no dev; obrigatória no perfil `postgres` | assinatura HS256, 32 caracteres ou mais |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | H2 em memória | PostgreSQL |
| `UPLOAD_DIR` | `uploads` | pasta das imagens (ignorada pelo git) |
| `SPRING_PROFILES_ACTIVE` | nenhum | `postgres` liga Flyway e validação do schema |
| `ADMIN_EMAIL` | vazio | só no perfil `postgres`: promove a `ADMIN` a conta já registrada com esse email |

## Endpoints

9 rotas.

| Método | Rota | Acesso | Resposta |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | público | 201 + token, 400, 409 (email em uso) |
| `POST` | `/api/auth/login` | público | 200 + token, 401 |
| `GET` | `/api/produtos?nome=&page=&size=&sort=` | público | 200 (página), 400 |
| `GET` | `/api/produtos/{id}` | público | 200, 404 |
| `GET` | `/api/produtos/imagens/{arquivo}` | público | 200, 404 |
| `POST` | `/api/produtos` | `ADMIN` | 201, 400, 401, 403 |
| `PUT` | `/api/produtos/{id}` | `ADMIN` | 200, 400, 401, 403, 404 |
| `DELETE` | `/api/produtos/{id}` | `ADMIN` | 204, 401, 403, 404 |
| `POST` | `/api/produtos/{id}/imagem` | `ADMIN` | 200, 401, 403, 404, 409 (tipo não aceito), 413 |

## Testes

```bash
./mvnw verify
```

15 testes de integração (`@SpringBootTest` + MockMvc, H2, perfil `test` com segredo JWT próprio e seeder desligado):

- `EcommerceApiIntegrationTest` (13): anônimo recebe 401 em ProblemDetail e admin cria; `USER` recebe 403 em ProblemDetail no POST, PUT, DELETE e upload; registro com `role` no JSON continua `USER`; `ADMIN_EMAIL` promove conta existente sem criar conta nova, e o token antigo passa a valer como admin; login; email duplicado; senha errada; senha curta; senha acima de 72 bytes em ASCII e com acento; login com senha longa não dá 500; `sort` inválido; `/error` público.
- `MigracaoFlywayTest` (1): aplica as migrations num H2 em modo PostgreSQL com `ddl-auto=validate`.
- `EcommerceApiApplicationTests` (1): o contexto sobe.

O upload não tem teste automatizado; foi conferido à mão contra o PostgreSQL (upload 200, download 200 com `image/png`, `..%2F..%2Fpom.xml` recusado com 400).

## O que ficou de fora

- O tipo da imagem é conferido pelo `Content-Type` que o cliente declara, não pelos bytes do arquivo.
- Trocar a imagem de um produto deixa a antiga no disco, e apagar o produto também.
- Sem rate limit no login, sem refresh token e sem forma de revogar um token antes de expirar. Rebaixar um admin vale na hora, porque o papel vem do banco, mas o token dele continua autenticando como `USER` até expirar.
- Não há rota para promover ou rebaixar usuário: o único caminho para `ADMIN` em produção é o `ADMIN_EMAIL` na subida.
- Não há carrinho, pedido nem pagamento: é um catálogo.

## Aprendizados

- Em Spring Security, erro interno é encaminhado para `/error`, e essa rota passa pelos mesmos filtros. Protegida, ela transforma bug do servidor em "não autenticado".
- Teste que cria produto com o token de qualquer usuário não protege nada: ele codificava a falha de autorização. O teste que importa é o que tenta com o papel errado e espera 403.
- Segredo com valor padrão no `application.properties` sobe em produção sem ninguém perceber. Tirar o padrão só do perfil de produção mantém o dev funcionando com `./mvnw spring-boot:run`.

## Licença

MIT, veja [LICENSE](LICENSE).
