package dev.guilherme.ecommerce.service;

import dev.guilherme.ecommerce.domain.Produto;
import dev.guilherme.ecommerce.dto.ProdutoRequest;
import dev.guilherme.ecommerce.dto.ProdutoResponse;
import dev.guilherme.ecommerce.exception.RecursoNaoEncontradoException;
import dev.guilherme.ecommerce.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final ArmazenamentoService armazenamento;

    public ProdutoService(ProdutoRepository repository, ArmazenamentoService armazenamento) {
        this.repository = repository;
        this.armazenamento = armazenamento;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto(request.nome(), request.descricao(), request.preco(), request.estoque());
        return ProdutoResponse.de(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(String nome, Pageable pageable) {
        Page<Produto> pagina = (nome == null || nome.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
        return pagina.map(ProdutoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return ProdutoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        produto.atualizar(request.nome(), request.descricao(), request.preco(), request.estoque());
        return ProdutoResponse.de(repository.save(produto));
    }

    @Transactional
    public ProdutoResponse enviarImagem(Long id, MultipartFile arquivo) {
        Produto produto = buscarEntidade(id);
        produto.definirImagem(armazenamento.salvar(arquivo));
        return ProdutoResponse.de(repository.save(produto));
    }

    @Transactional
    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Produto " + id + " não encontrado");
        }
        repository.deleteById(id);
    }

    private Produto buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado"));
    }
}
