package br.com.regulacheck.submissao.dto;

import br.com.regulacheck.submissao.entity.Submissao;
import java.time.temporal.ChronoUnit;

/**
 * Comprovante da submissao simulada (RC-17), como a API o publica em {@code POST} e
 * {@code GET /solicitacoes/{id}/submissoes-simuladas}. Sempre identificado como simulacao: destaque e nota fixos do
 * servidor, para que o cliente nao invente linguagem. Nao e documento clinico, nao e protocolo oficial, nao indica
 * fila, vaga, prazo nem aprovacao.
 *
 * @param solicitacaoId pedido, como texto
 * @param referencia referencia interna da simulacao (ex.: {@code SIM-2026-000012})
 * @param submetidaEm data e hora da conclusao em ISO-8601 UTC
 * @param simulada sempre {@code true}
 * @param destaque texto fixo {@link #DESTAQUE}
 * @param servico servico demonstrativo do pedido
 * @param versaoAnalisada analise usada na conclusao
 * @param nota texto fixo {@link #NOTA}
 */
public record SubmissaoResponse(String solicitacaoId, String referencia, String submetidaEm, boolean simulada,
        String destaque, Servico servico, VersaoAnalisada versaoAnalisada, String nota) {

    /** Destaque obrigatorio do comprovante. */
    public static final String DESTAQUE = "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS";

    /** Nota fixa do comprovante (texto de {@code mobile/design/textos.md}, E3.6). */
    public static final String NOTA = "Este comprovante é de demonstração. Não é documento clínico, não é protocolo "
            + "oficial e não coloca você em fila real.";

    /**
     * @param localidade localidade demonstrativa (ficticia)
     * @param especialidade especialidade demonstrativa
     * @param avisoFicticio aviso de que o protocolo e academico e nao e exigencia oficial
     */
    public record Servico(String localidade, String especialidade, String avisoFicticio) {
    }

    /**
     * @param analiseId analise vigente usada na conclusao, como texto
     * @param revisao revisao do conjunto documental avaliada
     * @param protocolo protocolo academico ficticio e versao aplicados
     */
    public record VersaoAnalisada(String analiseId, int revisao, Protocolo protocolo) {
    }

    /**
     * @param identificador nome do protocolo
     * @param versao versao aplicada
     */
    public record Protocolo(String identificador, String versao) {
    }

    /**
     * @param submissao submissao lida do banco
     * @return comprovante correspondente
     */
    public static SubmissaoResponse de(Submissao submissao) {
        return new SubmissaoResponse(Long.toString(submissao.solicitacaoId()), submissao.referencia(),
                submissao.submetidaEm().truncatedTo(ChronoUnit.MILLIS).toString(), true, DESTAQUE,
                new Servico(submissao.localidade(), submissao.especialidade(), submissao.avisoFicticio()),
                new VersaoAnalisada(Long.toString(submissao.analiseId()), submissao.revisaoAnalisada(),
                        new Protocolo(submissao.protocoloIdentificador(), submissao.protocoloVersao())),
                NOTA);
    }
}
