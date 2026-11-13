package br.com.regulacheck.protocolo;

import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio de protocolos em memoria para testes sem Oracle. Guarda tambem os protocolos inativos. */
public final class ProtocoloRepositoryMemoria implements ProtocoloRepository {

    /** Aviso sintetico igual ao da carga do banco. */
    public static final String AVISO =
            "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS.";

    private final Map<String, Protocolo> porProtocoloId = new LinkedHashMap<>();
    private final Map<String, Boolean> ativos = new LinkedHashMap<>();

    /** @return protocolo ativo padrao do MVP (v0.1), com o mesmo texto da carga sintetica */
    public static Protocolo padrao() {
        return new Protocolo(1, "PROTOCOLO-ACADEMICO-FICTICIO", "v0.1", "Município Exemplo",
                "Consulta ambulatorial em cardiologia", AVISO);
    }

    /** @return segundo protocolo sintetico, usado para provar a troca de servico */
    public static Protocolo segundo() {
        return new Protocolo(2, "PROTOCOLO-ACADEMICO-FICTICIO", "v0.2", "Município Exemplo",
                "Consulta ambulatorial em ortopedia", AVISO);
    }

    /**
     * @param protocolo servico a disponibilizar
     * @param ativo se aparece no catalogo e pode ser escolhido
     */
    public synchronized void adicionar(Protocolo protocolo, boolean ativo) {
        porProtocoloId.put(protocolo.protocoloId(), protocolo);
        ativos.put(protocolo.protocoloId(), ativo);
    }

    /** @param protocoloId servico a retirar do catalogo, como se o grupo o desativasse */
    public synchronized void desativar(String protocoloId) {
        ativos.put(protocoloId, false);
    }

    /** Remove todos os protocolos (catalogo vazio). */
    public synchronized void limpar() {
        porProtocoloId.clear();
        ativos.clear();
    }

    @Override
    public synchronized List<Protocolo> listarAtivos() {
        List<Protocolo> lista = new ArrayList<>();
        porProtocoloId.forEach((chave, protocolo) -> {
            if (ativos.get(chave)) {
                lista.add(protocolo);
            }
        });
        return lista;
    }

    @Override
    public synchronized Optional<Protocolo> buscarAtivoPorProtocoloId(String protocoloId) {
        return Boolean.TRUE.equals(ativos.get(protocoloId)) ? Optional.of(porProtocoloId.get(protocoloId))
                : Optional.empty();
    }
}
