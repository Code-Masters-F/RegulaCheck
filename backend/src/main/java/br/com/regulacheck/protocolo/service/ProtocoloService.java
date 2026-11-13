package br.com.regulacheck.protocolo.service;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import java.util.List;

/**
 * Casos de uso do catalogo de servicos (protocolos academicos ficticios): listar o que a paciente pode escolher
 * e resolver o {@code protocoloId} informado por ela.
 */
public class ProtocoloService {

    private final ProtocoloRepository protocolos;

    /** @param protocolos leitura dos protocolos */
    public ProtocoloService(ProtocoloRepository protocolos) {
        this.protocolos = protocolos;
    }

    /**
     * Lista os servicos que podem ser escolhidos.
     *
     * @return protocolos ativos; lista vazia (nao erro) quando nao ha nenhum
     * @throws ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    public List<Protocolo> listarAtivos() {
        return protocolos.listarAtivos();
    }

    /**
     * Resolve o servico escolhido, que precisa existir e estar ativo.
     *
     * @param protocoloId identificador publico ({@code identificador-versao})
     * @return o protocolo ativo
     * @throws ExcecaoNegocio 422 {@code REGRA_DE_NEGOCIO} com detalhe no campo {@code protocoloId} se o servico
     *     nao existe ou esta inativo; as duas situacoes tem a mesma resposta, para nao expor o catalogo interno
     */
    public Protocolo exigirAtivo(String protocoloId) {
        return protocolos.buscarAtivoPorProtocoloId(protocoloId).orElseThrow(() -> new ExcecaoNegocio(422,
                "REGRA_DE_NEGOCIO", "Verifique os campos informados.",
                List.of(new ErroPadrao.Detalhe("protocoloId", "Escolha um dos serviços da lista."))));
    }
}
