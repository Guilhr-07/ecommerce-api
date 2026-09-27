# E-commerce — API REST com JWT, Upload e Swagger (Spring Boot)

API de catálogo de e-commerce com **autenticação JWT**, **upload de imagens**,
**paginação/filtro** e **documentação OpenAPI (Swagger UI)**. Projeto marco do **Mês 4**
da trilha de backend. Fecha a sequência: [Tarefas](../gestor-tarefas-api) →
[Financeiro](../controle-financeiro-api) → **E-commerce**.

![tests](https://img.shields.io/badge/tests-passing-brightgreen) ![java](https://img.shields.io/badge/java-21-orange) ![auth](https://img.shields.io/badge/auth-JWT-purple)

## Stack

- **Java 21** + **Spring Boot 4.1**
- **Spring Security 6/7** + **JWT** (jjwt, HS256) · BCrypt
- Spring Data JPA · **PostgreSQL** (prod) / **H2** (dev e testes)
- Upload de arquivos (multipart) servido do disco
- **springdoc-openapi** → Swagger UI
- JUnit 5 + MockMvc

## Autenticação

Stateless por Bearer token. Fluxo: `register` ou `login` devolve um JWT; envie-o em
`Authorization: Bearer <token>` nas rotas protegidas.

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| `POST` | `/api/auth/register` | pública | cria usuário, devolve JWT |
| `POST` | `/api/auth/login` | pública | autentica, devolve JWT |

## Produtos

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| `GET` | `/api/produtos?nome=&page=&size=&sort=` | pública | lista paginada + filtro |
| `GET` | `/api/produtos/{id}` | pública | busca |
| `POST` | `/api/produtos` | **JWT** | cria |
| `PUT` | `/api/produtos/{id}` | **JWT** | atualiza |
| `DELETE` | `/api/produtos/{id}` | **JWT** | remove |
| `POST` | `/api/produtos/{id}/imagem` | **JWT** | upload (multipart, campo `arquivo`) |
| `GET` | `/api/produtos/imagens/{arquivo}` | pública | serve a imagem |

Erros em **ProblemDetail (RFC 7807)**: validação `400`, não autenticado `401`,
não encontrado `404`, regra de negócio `409`, upload grande `413`.

## Documentação (Swagger)

Com a API no ar: **http://localhost:8080/swagger-ui.html** (tem botão *Authorize*
para colar o Bearer token). OpenAPI JSON em `/v3/api-docs`.

## Como rodar

```bash
# H2 + dados de exemplo (admin@loja.dev / admin12345 e 3 produtos)
./mvnw spring-boot:run

# PostgreSQL
docker compose up            # API + Postgres
```

Exemplo rápido:

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

Requisições prontas em [`api.http`](api.http). Testes: `./mvnw test`.

## Configuração (variáveis de ambiente)

| Var | Default | Uso |
|-----|---------|-----|
| `JWT_SECRET` | (dev key) | **defina em produção** (≥ 32 chars) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | H2 em memória | PostgreSQL |
| `UPLOAD_DIR` | `uploads` | pasta das imagens |

## Estrutura

```
src/main/java/dev/guilherme/ecommerce
├── domain/        # Usuario, Role, Produto
├── repository/    # Spring Data JPA
├── dto/           # requests/responses (records)
├── security/      # JwtService, JwtAuthenticationFilter, SecurityConfig, UserDetailsService
├── service/       # AuthService, ProdutoService, ArmazenamentoService (upload)
├── web/           # controllers + GlobalExceptionHandler
├── config/        # OpenApiConfig, DataSeeder (dev)
└── exception/
```
