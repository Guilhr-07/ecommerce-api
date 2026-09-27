package dev.guilherme.ecommerce.service;

import dev.guilherme.ecommerce.exception.RecursoNaoEncontradoException;
import dev.guilherme.ecommerce.exception.RegraNegocioException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Salva e serve imagens de produto no disco local. */
@Service
public class ArmazenamentoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");

    private final Path raiz;

    public ArmazenamentoService(@Value("${app.upload.dir:uploads}") String dir) {
        this.raiz = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar o diretório de upload", e);
        }
    }

    /** Salva a imagem e devolve a URL relativa para servi-la. */
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Arquivo de imagem é obrigatório");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new RegraNegocioException("Tipo de imagem não suportado (use PNG, JPEG ou WebP)");
        }
        String extensao = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String nomeArquivo = UUID.randomUUID() + extensao;
        Path destino = raiz.resolve(nomeArquivo).normalize();
        if (!destino.startsWith(raiz)) {
            throw new RegraNegocioException("Caminho de arquivo inválido");
        }
        try {
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a imagem", e);
        }
        return "/api/produtos/imagens/" + nomeArquivo;
    }

    public Resource carregar(String nomeArquivo) {
        Path caminho = raiz.resolve(nomeArquivo).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada");
        }
        try {
            Resource resource = new UrlResource(caminho.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RecursoNaoEncontradoException("Imagem não encontrada: " + nomeArquivo);
            }
            return resource;
        } catch (IOException e) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada: " + nomeArquivo);
        }
    }
}
