package br.com.regulacheck.documento.repository;

import br.com.regulacheck.documento.entity.Documento;

/**
 * Desfecho do registro de um documento, decidido pelo repositorio sob o bloqueio da linha do pedido (ver
 * {@link DocumentoRepository#registrar}). O Service traduz cada situacao em resposta de negocio.
 *
 * <p>Alem do documento, carrega a versao do conjunto documental e a contagem de documentos <em>lidas dentro da
 * mesma transacao</em>, para que a resposta do envio descreva exatamente o estado em que o envio foi aplicado,
 * mesmo com outros envios simultaneos.
 *
 * @param situacao o que aconteceu
 * @param documento documento registrado ({@link Situacao#REGISTRADO}) ou ja existente ({@link Situacao#REPETIDO});
 *     {@code null} nas demais
 * @param versaoConjuntoDocumental revisao do pedido apos o desfecho; {@code 0} quando nao ha pedido
 * @param documentosDisponiveis documentos disponiveis do pedido apos o desfecho; {@code 0} quando nao ha pedido
 */
public record ResultadoRegistro(Situacao situacao, Documento documento, int versaoConjuntoDocumental,
        int documentosDisponiveis) {

    /** Situacoes possiveis do registro. */
    public enum Situacao {
        /** Nao existe pedido com esse id para essa paciente (inexistente ou de outra paciente). */
        NAO_ENCONTRADO,
        /** O pedido ja saiu de rascunho; nada foi gravado. */
        FORA_DE_RASCUNHO,
        /** O pedido ja tem o maximo de documentos disponiveis; nada foi gravado. */
        LIMITE,
        /** A chave de envio ja existe no pedido com outro conteudo ou outro tipo documental; nada foi gravado. */
        CONFLITO_CHAVE,
        /** A chave de envio ja existe com o mesmo conteudo e tipo: devolve o documento existente, sem gravar. */
        REPETIDO,
        /** Documento gravado, revisao do pedido incrementada e revisao registrada. */
        REGISTRADO
    }

    /** @return resultado sem documento: pedido nao encontrado */
    public static ResultadoRegistro naoEncontrado() {
        return new ResultadoRegistro(Situacao.NAO_ENCONTRADO, null, 0, 0);
    }

    /** @return resultado sem documento: pedido fora de rascunho */
    public static ResultadoRegistro foraDeRascunho() {
        return new ResultadoRegistro(Situacao.FORA_DE_RASCUNHO, null, 0, 0);
    }

    /** @return resultado sem documento: limite de documentos atingido */
    public static ResultadoRegistro limite() {
        return new ResultadoRegistro(Situacao.LIMITE, null, 0, 0);
    }

    /** @return resultado sem documento: chave de envio reutilizada com outro conteudo ou tipo */
    public static ResultadoRegistro conflitoChave() {
        return new ResultadoRegistro(Situacao.CONFLITO_CHAVE, null, 0, 0);
    }

    /**
     * @param existente documento ja registrado com essa chave
     * @param versao revisao atual do pedido
     * @param documentos documentos disponiveis do pedido
     * @return resultado de repeticao da mesma tentativa
     */
    public static ResultadoRegistro repetido(Documento existente, int versao, int documentos) {
        return new ResultadoRegistro(Situacao.REPETIDO, existente, versao, documentos);
    }

    /**
     * @param novo documento recem-gravado
     * @param versao revisao do pedido apos o envio
     * @param documentos documentos disponiveis do pedido apos o envio
     * @return resultado de registro aplicado
     */
    public static ResultadoRegistro registrado(Documento novo, int versao, int documentos) {
        return new ResultadoRegistro(Situacao.REGISTRADO, novo, versao, documentos);
    }
}
