package dev.guilherme.ecommerce.web;

import dev.guilherme.ecommerce.dto.ProdutoRequest;
import dev.guilherme.ecommerce.dto.ProdutoResponse;
import dev.guilherme.ecommerce.service.ArmazenamentoService;
import dev.guilherme.ecommerce.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Catálogo — leitura pública, escrita só para ADMIN (JWT)")
public class ProdutoController {

    private final ProdutoService service;
    private final ArmazenamentoService armazenamento;

    public ProdutoController(ProdutoService service, ArmazenamentoService armazenamento) {
        this.service = service;
        this.armazenamento = armazenamento;
    }

    @GetMapping
    @Operation(summary = "Lista produtos paginados (filtro opcional por nome)")
    public Page<ProdutoResponse> listar(@RequestParam(required = false) String nome,
                                        @PageableDefault(size = 12) Pageable pageable) {
        return service.listar(nome, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto por id")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria um produto (requer papel ADMIN)")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        ProdutoResponse criado = service.criar(request);
        URI location = uriBuilder.path("/api/produtos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza um produto (requer papel ADMIN)")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return service.atualizar(id, request);
    }

    @PostMapping(path = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Faz upload da imagem do produto (requer papel ADMIN)")
    public ProdutoResponse enviarImagem(@PathVariable Long id,
                                        @RequestParam("arquivo") MultipartFile arquivo) {
        return service.enviarImagem(id, arquivo);
    }

    @GetMapping("/imagens/{nomeArquivo}")
    @Operation(summary = "Serve a imagem de um produto")
    public ResponseEntity<Resource> imagem(@PathVariable String nomeArquivo) {
        Resource resource = armazenamento.carregar(nomeArquivo);
        return ResponseEntity.ok()
                .contentType(tipoPorExtensao(nomeArquivo))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um produto (requer papel ADMIN)")
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    private static MediaType tipoPorExtensao(String nome) {
        String n = nome.toLowerCase();
        if (n.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (n.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
