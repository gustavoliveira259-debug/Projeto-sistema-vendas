package br.com.aweb.sistema_vendas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository){
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    private void calcularValorTotal(Pedido pedido){
        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()){
            BigDecimal valorItem = item.getPreco_unitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            total = total.add(valorItem);
        }
        pedido.setValorTotal(total);
    }

    @Transactional
    public Pedido criarPedido(Cliente cliente){
        Pedido pedido = new Pedido(cliente);
        return pedidoRepository.save(pedido);
    }

    @Transactional 
    public void adicionarItem(Long pedidoId, Long produtoId, Integer quantidade){
        if (quantidade == null || quantidade <= 0){
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);
        Optional<Produto> optionalProduto = produtoRepository.findById(produtoId);

        if (!optionalPedido.isPresent()){
            throw new IllegalArgumentException("Pedido não encontrado.");
        }
        if (!optionalProduto.isPresent()){
            throw new IllegalArgumentException("Produto não encontrado.");
        }

        Pedido pedido = optionalPedido.get();
        Produto produto = optionalProduto.get();

        // Verifica se pedido está ativo.
        if (pedido.getStatus() != StatusPedido.ATIVO){
            throw new IllegalArgumentException("Não é possível alterar pedido cancelado.");
        }

        // Verifica estoque
        if (produto.getQuantidadeEstoque() < quantidade){
            throw new IllegalStateException("Estoque insuficiente para o produto " + produto.getNome());
        }

        // Cria o item do pedido
        ItemPedido item = new ItemPedido(produto, quantidade);
        item.setPedido(pedido);

        // Adiciona à lista do pedido
        pedido.getItens().add(item);

        // Atualiza estoque
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);

        // Recalcula valor total
        calcularValorTotal(pedido);

        // Salva Alterações
        pedidoRepository.save(pedido);
        produtoRepository.save(produto);

    }

    @Transactional
    public void removerItem(Long pedidoId, Long itemId){
        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);
        
        if(!optionalPedido.isPresent()){
            throw new IllegalArgumentException("Pedido não encontrado.");
        }

        Pedido pedido = optionalPedido.get();
        
        if (pedido.getStatus() != StatusPedido.ATIVO){
            throw new IllegalArgumentException("Não é possivel alterar pedido cancelado.");
        }

        ItemPedido itemParaRemover = null;
        for (ItemPedido item : pedido.getItens()){
            if (item.getId().equals(itemId)){
                itemParaRemover = item;
                break;
            }
        }

        if (itemParaRemover == null){
            throw new IllegalArgumentException("Item não encontrado no pedido");
        }

        // Devolve Estoque
        Produto produto = itemParaRemover.getProduto();
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + itemParaRemover.getQuantidade());

        pedido.getItens().remove(itemParaRemover);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
        produtoRepository.save(produto);
    }

    @Transactional
    public void cancelarPedido(Long pedidoId){
        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);

        if (!optionalPedido.isPresent()){
            throw new IllegalArgumentException("Pedido não encontrado");
        }

        Pedido pedido = optionalPedido.get();

        if (pedido.getStatus() == StatusPedido.CANCELADO){
            throw new IllegalArgumentException("Pedido já está cancelado.");
        }

        // Devolve todos os itens ao estoque
        for(ItemPedido item : pedido.getItens()){
            Produto produto = item.getProduto();
            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }

        // Altera status para cancelado
        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    // Valida se o pedido pode ser finalizado
    @Transactional(readOnly = true)
    public void finalizarPedido(Long pedidoId){
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.ATIVO){
            throw new IllegalArgumentException("Não é possível finalizar pedido cancelado.");
        }
        if (pedido.getItens().isEmpty()){
            throw new IllegalArgumentException("Pedido precisa ter ao menos um item.");
        }
    }

    // Read - buscar pedido por ID
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Long id){
        return pedidoRepository.findById(id);
    }

    // Read - Listar todos os pedidos
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos(){
        return pedidoRepository.findAll();
    }

    // Read - Listar pedido status
    @Transactional(readOnly = true)
    public List<Pedido> listarPorStatus(StatusPedido status){
        return pedidoRepository.findByStatus(status);
    }
}
