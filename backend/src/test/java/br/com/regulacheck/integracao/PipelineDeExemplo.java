package br.com.regulacheck.integracao;

import br.com.regulacheck.integracao.ValidadorDeSaidaEstruturada.Item;
import br.com.regulacheck.integracao.ValidadorDeSaidaEstruturada.Resultado;
import br.com.regulacheck.usuario.entity.Perfil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline de exemplo, <strong>so de teste</strong>, que mostra o uso correto das protecoes de conteudo nao
 * confiavel. O motor de regras real, a extracao e o resumo ainda nao existem (historias futuras); quando
 * existirem, devem repetir {@code InjecaoEmDocumentoTest} sobre o caminho real.
 *
 * <p>Papeis: (1) um "motor" deterministico decide o estado de cada regra a partir de campos extraidos,
 * nunca do adaptador; (2) o texto do documento vai ao adaptador somente por
 * {@link TextoNaoConfiavel#delimitar}; (3) a saida do adaptador passa por {@link ValidadorDeSaidaEstruturada} e
 * so e usada se cada item concorda com o motor; (4) perfil e estado da solicitacao nao dependem do texto.
 */
final class PipelineDeExemplo {

    static final Set<String> REGRAS = Set.of("R01_CNS", "R02_DATA");
    static final String RESUMO_PADRAO = "Resumo padrao (simulado): confira as pendencias listadas.";

    private static final Pattern CNS = Pattern.compile("CNS:\\s*(\\d{15})");
    private static final Pattern DATA = Pattern.compile("DATA:\\s*(\\d{2}/\\d{2}/\\d{4})");

    /** Resultado do pipeline: o que o documento <em>nao</em> pode alterar. */
    record Saida(Map<String, String> criterios, Perfil perfil, String estadoSolicitacao, List<String> explicacoes,
            boolean saidaDoAdaptadorRejeitada) {
    }

    private final ValidadorDeSaidaEstruturada validador = new ValidadorDeSaidaEstruturada(REGRAS,
            ValidadorDeSaidaEstruturada.ESTADOS_PADRAO, 200);

    /**
     * @param documento texto extraido do arquivo (nao confiavel)
     * @param perfilDaConta perfil da conta que enviou, vindo da sessao
     * @param adaptador adaptador de resumo (recebe a mensagem delimitada, devolve JSON)
     * @return criterios, perfil, estado e explicacoes
     */
    Saida processar(TextoNaoConfiavel documento, Perfil perfilDaConta, Function<String, String> adaptador) {
        Map<String, String> criterios = new LinkedHashMap<>();
        criterios.put("R01_CNS", CNS.matcher(documento.texto()).find() ? "ATENDE" : "PENDENTE");
        criterios.put("R02_DATA", DATA.matcher(documento.texto()).find() ? "ATENDE" : "PENDENTE");

        String resposta = adaptador.apply(documento.delimitar("doc-1"));
        Resultado validada = validador.validar(resposta);
        boolean rejeitada = false;
        List<String> explicacoes = List.of(RESUMO_PADRAO);
        if (validada instanceof Resultado.Valida valida) {
            boolean concorda = valida.itens().stream().allMatch(i -> i.estado().equals(criterios.get(i.codigoRegra())));
            if (concorda) {
                explicacoes = valida.itens().stream().map(Item::texto).toList();
            } else {
                rejeitada = true;
            }
        } else {
            rejeitada = true;
        }
        // Perfil e estado nao sao lidos do documento nem da saida do adaptador.
        return new Saida(Map.copyOf(criterios), perfilDaConta, "EM_ANALISE", explicacoes, rejeitada);
    }

    static Matcher campo(Pattern p, String texto) {
        return p.matcher(texto);
    }
}
