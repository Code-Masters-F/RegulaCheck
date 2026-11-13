package br.com.regulacheck.extracao.dto;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.extracao.entity.MotivoDivergencia;
import br.com.regulacheck.extracao.entity.TipoDecisao;
import br.com.regulacheck.extracao.repository.DecisaoNova;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Corpo de {@code PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia}: as decisoes da paciente sobre os
 * campos lidos. O corpo e <strong>fechado</strong>: a paciente so confirma ou aponta divergencia, nunca digita
 * valor nem corrige a transcricao (CLAUDE.md, secao 11). Por isso o corpo e lido como {@link JsonNode} e validado a
 * mao: o mapeador do projeto ignora propriedades desconhecidas, e aqui qualquer chave alem de {@code documentoId},
 * {@code codigo}, {@code decisao} e {@code motivo} (como {@code valor}) e recusada, em vez de ignorada em silencio.
 *
 * <p>Nenhuma mensagem de erro reproduz o conteudo recebido (nome de chave, valor ou texto digitado).
 */
public final class ConferenciaRequest {

    /** Maximo de decisoes por requisicao: 3 documentos vezes 6 campos. */
    public static final int MAXIMO_DECISOES = 18;

    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");
    private static final Set<String> CHAVES_DO_ITEM = Set.of("documentoId", "codigo", "decisao", "motivo");

    private ConferenciaRequest() {
    }

    /**
     * Valida o corpo e o converte em decisoes a registrar.
     *
     * @param corpo raiz do JSON recebido; pode ser {@code null}
     * @return de 1 a {@value #MAXIMO_DECISOES} decisoes, sem par (documento, campo) repetido
     * @throws ExcecaoNegocio 400 {@code REQUISICAO_INVALIDA} com detalhe no item e campo se a lista estiver ausente,
     *     vazia ou grande demais, se houver chave desconhecida, valor fora da lista, motivo ausente em divergencia
     *     ou presente em confirmacao, par repetido ou identificador de documento malformado
     */
    public static List<DecisaoNova> validar(JsonNode corpo) {
        if (corpo == null || !corpo.isObject()) {
            throw recusa("corpo", "Envie um objeto com a lista de decisões.");
        }
        corpo.fieldNames().forEachRemaining(chave -> {
            if (!"decisoes".equals(chave)) {
                throw recusa("corpo", "Há um campo não permitido. Só é aceita a lista de decisões.");
            }
        });
        JsonNode lista = corpo.get("decisoes");
        if (lista == null || !lista.isArray() || lista.isEmpty()) {
            throw recusa("decisoes", "Informe de 1 a " + MAXIMO_DECISOES + " decisões.");
        }
        if (lista.size() > MAXIMO_DECISOES) {
            throw recusa("decisoes", "Informe no máximo " + MAXIMO_DECISOES + " decisões.");
        }
        List<DecisaoNova> decisoes = new ArrayList<>();
        Set<String> pares = new HashSet<>();
        for (int i = 0; i < lista.size(); i++) {
            DecisaoNova decisao = item(lista.get(i), "decisoes[" + i + "]");
            if (!pares.add(decisao.documentoId() + "/" + decisao.codigo().name())) {
                throw recusa("decisoes[" + i + "]", "Há mais de uma decisão para o mesmo campo.");
            }
            decisoes.add(decisao);
        }
        return decisoes;
    }

    private static DecisaoNova item(JsonNode item, String caminho) {
        if (item == null || !item.isObject()) {
            throw recusa(caminho, "Cada decisão deve ser um objeto.");
        }
        item.fieldNames().forEachRemaining(chave -> {
            if (!CHAVES_DO_ITEM.contains(chave)) {
                throw recusa(caminho, "Há um campo não permitido. Só são aceitos documentoId, codigo, decisao e "
                        + "motivo: não é possível informar valores.");
            }
        });
        long documentoId = documentoId(item.get("documentoId"), caminho + ".documentoId");
        CodigoCampo codigo = enumeracao(item.get("codigo"), CodigoCampo.class, caminho + ".codigo");
        TipoDecisao tipo = enumeracao(item.get("decisao"), TipoDecisao.class, caminho + ".decisao");
        JsonNode motivoJson = item.get("motivo");
        boolean temMotivo = motivoJson != null && !motivoJson.isNull();
        MotivoDivergencia motivo = null;
        if (tipo == TipoDecisao.DIVERGENTE) {
            if (!temMotivo) {
                throw recusa(caminho + ".motivo", "Informe o motivo da divergência.");
            }
            motivo = enumeracao(motivoJson, MotivoDivergencia.class, caminho + ".motivo");
        } else if (temMotivo) {
            throw recusa(caminho + ".motivo", "Uma confirmação não leva motivo.");
        }
        return new DecisaoNova(documentoId, codigo, tipo, motivo);
    }

    private static long documentoId(JsonNode no, String campo) {
        if (no == null || !no.isTextual() || !ID.matcher(no.asText()).matches()) {
            throw recusa(campo, "Informe o identificador do documento como texto numérico.");
        }
        return Long.parseLong(no.asText());
    }

    private static <E extends Enum<E>> E enumeracao(JsonNode no, Class<E> tipo, String campo) {
        if (no == null || !no.isTextual()) {
            throw recusa(campo, "Informe um valor da lista permitida.");
        }
        for (E valor : tipo.getEnumConstants()) {
            if (valor.name().equals(no.asText())) {
                return valor;
            }
        }
        throw recusa(campo, "Informe um valor da lista permitida.");
    }

    private static ExcecaoNegocio recusa(String campo, String mensagem) {
        return new ExcecaoNegocio(400, "REQUISICAO_INVALIDA", "Verifique os campos informados.",
                List.of(new ErroPadrao.Detalhe(campo, mensagem)));
    }
}
