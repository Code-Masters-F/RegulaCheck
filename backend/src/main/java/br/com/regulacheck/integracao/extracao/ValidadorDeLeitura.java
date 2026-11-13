package br.com.regulacheck.integracao.extracao;

import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Valida a saida de qualquer {@link LeitorDeDocumento} (simulado hoje, real no futuro) contra um contrato
 * <strong>fechado</strong>: saida fora do contrato e rejeitada, nunca "corrigida", e nunca e apresentada como
 * leitura confiavel. O motivo da rejeicao nao reproduz o conteudo recebido.
 *
 * <p>Regras: {@code LIDO} traz os seis codigos, cada um uma unica vez; {@code PROTEGIDO} e {@code LEITURA_VAZIA}
 * nao trazem campo algum (e, portanto, nunca declaram "nao localizado"); valor so em {@code IDENTIFICADO}, nao
 * vazio, ate 200 caracteres e sem caracteres de controle; origem so em campo identificado, com pagina {@code >= 1}
 * (e {@code <=} paginas do arquivo) apenas em PDF e trecho de ate 200 caracteres.
 */
public final class ValidadorDeLeitura {

    /** Tamanho maximo de valor e de trecho. */
    public static final int TAMANHO_MAXIMO = 200;

    /** Motivos de rejeicao (lista fechada, sem eco do conteudo). */
    public enum Motivo {
        LEITURA_AUSENTE, CAMPOS_EM_LEITURA_NAO_LIDA, CAMPOS_INCOMPLETOS, CAMPO_REPETIDO, CAMPO_INVALIDO,
        VALOR_EM_CAMPO_SEM_VALOR, VALOR_AUSENTE, VALOR_INVALIDO, ORIGEM_INVALIDA
    }

    private ValidadorDeLeitura() {
    }

    /**
     * Confere uma leitura.
     *
     * @param leitura saida do leitor
     * @param tipo formato do arquivo lido; {@code null} dispensa a conferencia de pagina por formato (usado so ao
     *     carregar o catalogo do leitor simulado)
     * @param paginas paginas do arquivo (ignorado se {@code tipo} for {@code null})
     * @return o motivo da rejeicao, ou vazio se a leitura respeita o contrato
     */
    public static Optional<Motivo> verificar(LeituraDeDocumento leitura, TipoArquivo tipo, int paginas) {
        if (leitura == null || leitura.situacao() == null) {
            return Optional.of(Motivo.LEITURA_AUSENTE);
        }
        if (leitura.situacao() != SituacaoLeitura.LIDO) {
            return leitura.campos().isEmpty() ? Optional.empty() : Optional.of(Motivo.CAMPOS_EM_LEITURA_NAO_LIDA);
        }
        Set<CodigoCampo> vistos = EnumSet.noneOf(CodigoCampo.class);
        for (CampoLido campo : leitura.campos()) {
            if (campo == null || campo.codigo() == null || campo.situacao() == null) {
                return Optional.of(Motivo.CAMPO_INVALIDO);
            }
            if (!vistos.add(campo.codigo())) {
                return Optional.of(Motivo.CAMPO_REPETIDO);
            }
            Optional<Motivo> problema = verificarCampo(campo, tipo, paginas);
            if (problema.isPresent()) {
                return problema;
            }
        }
        return vistos.size() == CodigoCampo.values().length ? Optional.empty() : Optional.of(Motivo.CAMPOS_INCOMPLETOS);
    }

    private static Optional<Motivo> verificarCampo(CampoLido campo, TipoArquivo tipo, int paginas) {
        if (campo.situacao() != SituacaoCampo.IDENTIFICADO) {
            if (campo.valor() != null) {
                return Optional.of(Motivo.VALOR_EM_CAMPO_SEM_VALOR);
            }
            return campo.pagina() == null && campo.trecho() == null ? Optional.empty()
                    : Optional.of(Motivo.ORIGEM_INVALIDA);
        }
        if (campo.valor() == null || campo.valor().isBlank()) {
            return Optional.of(Motivo.VALOR_AUSENTE);
        }
        if (!textoValido(campo.valor())) {
            return Optional.of(Motivo.VALOR_INVALIDO);
        }
        if (campo.trecho() != null && (campo.trecho().isBlank() || !textoValido(campo.trecho()))) {
            return Optional.of(Motivo.ORIGEM_INVALIDA);
        }
        if (campo.pagina() != null) {
            boolean paginaValida = campo.pagina() >= 1
                    && (tipo == null || (tipo == TipoArquivo.PDF && campo.pagina() <= paginas));
            if (!paginaValida) {
                return Optional.of(Motivo.ORIGEM_INVALIDA);
            }
        }
        return Optional.empty();
    }

    private static boolean textoValido(String texto) {
        return texto.length() <= TAMANHO_MAXIMO && texto.chars().noneMatch(Character::isISOControl);
    }

    /**
     * Exige uma leitura valida.
     *
     * @param leitura saida do leitor
     * @param tipo formato do arquivo lido
     * @param paginas paginas do arquivo
     * @throws LeituraIndisponivelException se a saida estiver fora do contrato (a mensagem traz so o motivo)
     */
    public static void exigir(LeituraDeDocumento leitura, TipoArquivo tipo, int paginas)
            throws LeituraIndisponivelException {
        Optional<Motivo> problema = verificar(leitura, tipo, paginas);
        if (problema.isPresent()) {
            throw new LeituraIndisponivelException("Saida do leitor fora do contrato: " + problema.get());
        }
    }
}
