package dev.guilherme.ecommerce.service;

import dev.guilherme.ecommerce.domain.Role;
import dev.guilherme.ecommerce.domain.Usuario;
import dev.guilherme.ecommerce.dto.AuthResponse;
import dev.guilherme.ecommerce.dto.LoginRequest;
import dev.guilherme.ecommerce.dto.RegisterRequest;
import dev.guilherme.ecommerce.exception.CredenciaisInvalidasException;
import dev.guilherme.ecommerce.exception.RegraNegocioException;
import dev.guilherme.ecommerce.repository.UsuarioRepository;
import dev.guilherme.ecommerce.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository repository, PasswordEncoder encoder, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraNegocioException("Email já cadastrado");
        }
        Usuario usuario = new Usuario(request.nome(), request.email(),
                encoder.encode(request.senha()), Role.USER);
        repository.save(usuario);
        return AuthResponse.bearer(jwtService.gerarToken(usuario), jwtService.getExpiracaoSegundos());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = repository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha inválidos"));
        if (!encoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Email ou senha inválidos");
        }
        return AuthResponse.bearer(jwtService.gerarToken(usuario), jwtService.getExpiracaoSegundos());
    }
}
