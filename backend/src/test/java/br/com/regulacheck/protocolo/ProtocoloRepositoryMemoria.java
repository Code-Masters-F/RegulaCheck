package br.com.regulacheck.protocolo;

import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.protocolo.repository.ProtocoloRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de protocolos em memoria para testes sem Oracle. Guarda tambem os protocolos inativos e as regras de
 * cada versao; a versao padrao (v0.1, id 1) ja vem com as seis regras da carga sintetica.
 */
public final class ProtocoloRepositoryMemoria implements ProtocoloRepository {

    /** Aviso sintetico igual ao da carga do banco. */
    public static final String AVISO =
            "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS.";

    private final Map<String, Protocolo> porProtocoloId = new LinkedHashMap<>();
    private final Map<String, Boolean> ativos = new LinkedHashMap<>();
    private final Map<Long, List<RegraDoProtocolo>> regrasPorVersao = new LinkedHashMap<>(
            Map.of(padrao().id(), regrasV01()));

    /**
     * Regras da v0.1 com os mesmos codigos, textos, naturezas, responsaveis e ordem da carga
     * {@code database/scripts/02_carga_sintetica.sql}.
     *
     * @return as seis regras, na ordem da versao
     */
    public static List<RegraDoProtocolo> regrasV01() {
        return List.of(
                new RegraDoProtocolo("RC-R01", "Encaminhamento presente",
                        "Documento de encaminhamento para a especialidade", NaturezaRegra.OBRIGATORIA, null,
                        "Não encontramos o encaminhamento. Envie o documento emitido pela sua unidade.",
                        ResponsavelCorrecao.PACIENTE, 1),
                new RegraDoProtocolo("RC-R02", "Identificação coerente entre documentos",
                        "Nome e data de nascimento iguais nos documentos enviados", NaturezaRegra.OBRIGATORIA, null,
                        "Nome ou data de nascimento diferem entre os documentos. Confira os dados.",
                        ResponsavelCorrecao.PACIENTE, 2),
                new RegraDoProtocolo("RC-R03", "Documento de identificação",
                        "Documento de identificação ou cartão do SUS (sintéticos) legível", NaturezaRegra.OBRIGATORIA,
                        null, "Envie um documento de identificação ou o cartão do SUS.",
                        ResponsavelCorrecao.PACIENTE, 3),
                new RegraDoProtocolo("RC-R04", "Motivo do encaminhamento informado",
                        "Campo de motivo/justificativa preenchido no encaminhamento", NaturezaRegra.OBRIGATORIA, null,
                        "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento.",
                        ResponsavelCorrecao.UNIDADE_EMISSORA, 4),
                new RegraDoProtocolo("RC-R05", "Encaminhamento dentro da validade",
                        "Data de emissão com no máximo 180 dias de antiguidade (validade demonstrativa)",
                        NaturezaRegra.OBRIGATORIA, null,
                        "A data do encaminhamento está fora da validade deste protocolo de teste. "
                                + "Peça um novo à unidade emissora.",
                        ResponsavelCorrecao.UNIDADE_EMISSORA, 5),
                new RegraDoProtocolo("RC-R06", "Autorização do responsável legal",
                        "Autorização assinada do responsável", NaturezaRegra.CONDICIONAL,
                        "Paciente com menos de 18 anos na data do envio",
                        "Para pacientes menores de 18 anos, envie a autorização do responsável.",
                        ResponsavelCorrecao.PACIENTE, 6));
    }

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

    /**
     * Troca as regras de uma versao (RC-12), por exemplo para simular versao sem regras ou com regra desconhecida.
     *
     * @param protocoloId identificador interno da versao
     * @param regras regras da versao (pode ser vazia)
     */
    public synchronized void definirRegras(long protocoloId, List<RegraDoProtocolo> regras) {
        regrasPorVersao.put(protocoloId, List.copyOf(regras));
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

    @Override
    public synchronized List<RegraDoProtocolo> listarRegras(long protocoloId) {
        List<RegraDoProtocolo> regras = new ArrayList<>(regrasPorVersao.getOrDefault(protocoloId, List.of()));
        regras.sort(Comparator.comparingInt(RegraDoProtocolo::ordem).thenComparing(RegraDoProtocolo::codigo));
        return regras;
    }
}
