package dev.guilherme.ecommerce.dto;

public record AuthResponse(String token, String tipo, long expiraEmSegundos) {

    public static AuthResponse bearer(String token, long expiraEmSegundos) {
        return new AuthResponse(token, "Bearer", expiraEmSegundos);
    }
}
