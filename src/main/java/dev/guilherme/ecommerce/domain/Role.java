package dev.guilherme.ecommerce.domain;

/** Papéis de usuário. A autoridade no Spring Security é "ROLE_" + nome. */
public enum Role {
    USER,
    ADMIN
}
