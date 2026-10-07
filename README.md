# E-commerce API

[![CI](https://github.com/Guilhr-07/ecommerce-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Guilhr-07/ecommerce-api/actions/workflows/ci.yml) | Case completo: [guilherme-portfolio.dev/projetos/ecommerce-api](https://guilherme-portfolio.dev/projetos/ecommerce-api)

API de catálogo de loja com login. Qualquer pessoa vê os produtos; só administradores cadastram, editam e enviam imagens.

**Stack:** Java 21, Spring Boot 4, Spring Security (JWT), PostgreSQL, Flyway, Docker, JUnit 5

## Como rodar

Pré-requisito: JDK 21 ou mais novo.

```bash
./mvnw spring-boot:run
```

Sobe com banco em memória, um admin de exemplo (`admin@loja.dev` / `admin12345`) e 3 produtos. Documentação interativa em `http://localhost:8080/swagger-ui.html`.

Com PostgreSQL no Docker:

```bash
cp .env.example .env    # troque o JWT_SECRET
docker compose up --build
```

Como criar o primeiro administrador em produção está no [MANUAL](MANUAL.md).

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

15 testes de integração, rodando no CI a cada push.

## Mais detalhes

Como funciona por dentro, decisões técnicas, limites conhecidos e aprendizados: [MANUAL.md](MANUAL.md).

## Licença

MIT, veja [LICENSE](LICENSE).
