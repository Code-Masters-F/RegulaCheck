package br.com.regulacheck.validacao.motor;

import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.validacao.model.AvaliacaoDeRegras;
import br.com.regulacheck.validacao.model.EntradaAvaliacao;
import br.com.regulacheck.validacao.model.Pendencia;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.ResultadoDeRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import br.com.regulacheck.validacao.model.Veredito;
import br.com.regulacheck.validacao.regra.RegraAutorizacaoDoResponsavel;
import br.com.regulacheck.validacao.regra.RegraDocumental;
import br.com.regulacheck.validacao.regra.RegraDocumentoDeIdentificacao;
import br.com.regulacheck.validacao.regra.RegraEncaminhamentoPresente;
import br.com.regulacheck.validacao.regra.RegraIdentificacaoCoerente;
import br.com.regulacheck.validacao.regra.RegraMotivoInformado;
import br.com.regulacheck.validacao.regra.RegraValidadeDoEncaminhamento;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Motor de regras deterministico (RC-12): aplica as regras de uma versao do protocolo a uma entrada e devolve um
 * resultado por regra, as pendencias e o resultado geral.
 *
 * <ul>
 *   <li><strong>Versionado:</strong> as regras aplicadas sao as da versao recebida (codigo, textos, natureza,
 *       responsavel e ordem vem de {@code RC_REGRA}); a logica de cada uma e a {@link RegraDocumental} do mesmo
 *       codigo. A versao e conferida por inteiro <em>antes</em> de avaliar: sem regras, regra sem implementacao,
 *       natureza divergente ou codigo repetido recusam a versao ({@link ProtocoloNaoSuportadoException}), sem
 *       resultado parcial.</li>
 *   <li><strong>Completo:</strong> todas as regras sao avaliadas; uma pendencia nao interrompe as demais.</li>
 *   <li><strong>Pendencias:</strong> regra pendente usa a mensagem e o responsavel da versao; nao verificavel usa
 *       a {@link #MENSAGEM_NAO_VERIFICAVEL mensagem padrao do protocolo}, o motivo e a paciente como responsavel.</li>
 * </ul>
 *
 * <p>Sem estado mutavel: a mesma instancia pode ser usada por varias requisicoes ao mesmo tempo.
 */
public final class MotorDeRegras {

    /**
     * Mensagem padrao de "nao verificavel" do protocolo academico v0.1 ({@code docs/protocolo-academico.md}); a
     * mesma da carga {@code 02_carga_sintetica.sql}.
     */
    public static final String MENSAGEM_NAO_VERIFICAVEL = "Não conseguimos ler esta informação no documento enviado. "
            + "Isso não significa que ela esteja faltando. Envie uma foto ou arquivo mais nítido ou confira a "
            + "transcrição.";

    private final Map<String, RegraDocumental> porCodigo;

    /**
     * @param implementacoes logica das regras conhecidas, uma por codigo
     * @throws IllegalArgumentException se dois codigos se repetirem
     */
    public MotorDeRegras(List<RegraDocumental> implementacoes) {
        Map<String, RegraDocumental> mapa = new LinkedHashMap<>();
        for (RegraDocumental regra : implementacoes) {
            if (mapa.put(regra.codigo(), regra) != null) {
                throw new IllegalArgumentException("Implementacao repetida para " + regra.codigo());
            }
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    /** @return motor com as seis regras do protocolo academico ficticio (RC-R01 a RC-R06) */
    public static MotorDeRegras doProtocoloAcademico() {
        return new MotorDeRegras(List.of(new RegraEncaminhamentoPresente(), new RegraIdentificacaoCoerente(),
                new RegraDocumentoDeIdentificacao(), new RegraMotivoInformado(), new RegraValidadeDoEncaminhamento(),
                new RegraAutorizacaoDoResponsavel()));
    }

    /**
     * Avalia a entrada com as regras da versao.
     *
     * @param regrasDaVersao regras da versao do protocolo do pedido (qualquer ordem; a avaliacao segue
     *     {@code ordem}, depois codigo)
     * @param entrada documentos e data de referencia
     * @return um resultado por regra, as pendencias e o resultado geral
     * @throws ProtocoloNaoSuportadoException se a versao nao puder ser aplicada por inteiro
     */
    public AvaliacaoDeRegras avaliar(List<RegraDoProtocolo> regrasDaVersao, EntradaAvaliacao entrada) {
        Objects.requireNonNull(entrada, "entrada");
        List<RegraDoProtocolo> ordenadas = conferirVersao(regrasDaVersao);
        List<ResultadoDeRegra> resultados = new ArrayList<>();
        List<Pendencia> pendencias = new ArrayList<>();
        for (RegraDoProtocolo regra : ordenadas) {
            Veredito veredito = porCodigo.get(regra.codigo()).avaliar(entrada);
            if (regra.natureza() == NaturezaRegra.OBRIGATORIA && veredito.resultado() == Resultado.NAO_APLICAVEL) {
                // Defesa contra erro de implementacao: so a condicional pode deixar de se aplicar.
                throw new IllegalStateException("Regra obrigatoria nao pode ser nao aplicavel: " + regra.codigo());
            }
            resultados.add(new ResultadoDeRegra(regra, veredito));
            if (veredito.resultado().geraPendencia()) {
                pendencias.add(pendencia(regra, veredito));
            }
        }
        ResultadoGeral geral = ResultadoGeral.de(resultados.stream().map(ResultadoDeRegra::resultado).toList());
        return new AvaliacaoDeRegras(resultados, pendencias, geral);
    }

    /** Confere a versao inteira antes de avaliar qualquer regra e devolve as regras na ordem de avaliacao. */
    private List<RegraDoProtocolo> conferirVersao(List<RegraDoProtocolo> regrasDaVersao) {
        if (regrasDaVersao == null || regrasDaVersao.isEmpty()) {
            throw new ProtocoloNaoSuportadoException(ProtocoloNaoSuportadoException.Motivo.SEM_REGRAS, null);
        }
        Set<String> vistos = new HashSet<>();
        for (RegraDoProtocolo regra : regrasDaVersao) {
            if (!vistos.add(regra.codigo())) {
                throw new ProtocoloNaoSuportadoException(ProtocoloNaoSuportadoException.Motivo.REGRA_REPETIDA,
                        regra.codigo());
            }
            RegraDocumental implementacao = porCodigo.get(regra.codigo());
            if (implementacao == null) {
                throw new ProtocoloNaoSuportadoException(ProtocoloNaoSuportadoException.Motivo.REGRA_DESCONHECIDA,
                        regra.codigo());
            }
            if (implementacao.natureza() != regra.natureza()) {
                throw new ProtocoloNaoSuportadoException(ProtocoloNaoSuportadoException.Motivo.NATUREZA_DIVERGENTE,
                        regra.codigo());
            }
        }
        List<RegraDoProtocolo> ordenadas = new ArrayList<>(regrasDaVersao);
        ordenadas.sort(Comparator.comparingInt(RegraDoProtocolo::ordem).thenComparing(RegraDoProtocolo::codigo));
        return ordenadas;
    }

    private static Pendencia pendencia(RegraDoProtocolo regra, Veredito veredito) {
        if (veredito.resultado() == Resultado.PENDENTE) {
            return new Pendencia(regra.codigo(), regra.descricao(), Resultado.PENDENTE, regra.mensagem(), null,
                    veredito.evidencias(), regra.responsavel());
        }
        // Nao verificavel: falta evidencia legivel, nao requisito. A acao (arquivo mais nitido ou conferir a
        // transcricao) e sempre da paciente, mesmo quando a regra, se pendente, seria da unidade emissora.
        return new Pendencia(regra.codigo(), regra.descricao(), Resultado.NAO_VERIFICAVEL, MENSAGEM_NAO_VERIFICAVEL,
                veredito.motivo(), veredito.evidencias(), ResponsavelCorrecao.PACIENTE);
    }
}
