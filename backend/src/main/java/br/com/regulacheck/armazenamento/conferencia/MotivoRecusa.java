package br.com.regulacheck.armazenamento.conferencia;

import br.com.regulacheck.common.exception.ErroPadrao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.util.List;

/**
 * Lista fechada de motivos pelos quais um arquivo (ou pedido) e recusado. Cada motivo ja carrega o
 * status HTTP, o codigo estavel e uma mensagem que orienta a pessoa sem citar caminho, nome interno
 * nem detalhe tecnico (contrato em {@code specs/006-protecao-entradas/contracts/api-protecoes.md}).
 */
public enum MotivoRecusa {

    /** Conteudo nao e PDF, JPEG nem PNG. */
    TIPO_NAO_SUPORTADO(415, "TIPO_NAO_SUPORTADO", null, "Envie um arquivo PDF, JPEG ou PNG."),

    /** Extensao ausente, nao permitida ou diferente do tipo real do conteudo. */
    EXTENSAO_INCOMPATIVEL(415, "EXTENSAO_INCOMPATIVEL", null,
            "A extensão do arquivo não corresponde ao seu conteúdo."),

    /** Arquivo acima do tamanho maximo. */
    TAMANHO_EXCEDIDO(413, "ARQUIVO_MUITO_GRANDE", null, "O arquivo excede 5 MB."),

    /** PDF com paginas demais. */
    EXCESSO_DE_PAGINAS(400, "REQUISICAO_INVALIDA", "arquivo", "O PDF pode ter no máximo 5 páginas."),

    /** PDF protegido por senha ou criptografia. */
    PDF_PROTEGIDO(400, "REQUISICAO_INVALIDA", "arquivo",
            "PDF protegido por senha. Envie uma versão sem proteção ou um novo arquivo."),

    /** Arquivo vazio, truncado, corrompido ou imagem grande demais. */
    ARQUIVO_ILEGIVEL(400, "REQUISICAO_INVALIDA", "arquivo",
            "Não foi possível ler o arquivo. Envie um novo arquivo."),

    /** Mais arquivos que o permitido no pedido. */
    EXCESSO_DE_ARQUIVOS(400, "REQUISICAO_INVALIDA", "arquivos", "No máximo 3 arquivos por pedido.");

    private final int status;
    private final String codigo;
    private final String campo;
    private final String mensagem;

    MotivoRecusa(int status, String codigo, String campo, String mensagem) {
        this.status = status;
        this.codigo = codigo;
        this.campo = campo;
        this.mensagem = mensagem;
    }

    /** @return status HTTP da resposta de recusa */
    public int status() {
        return status;
    }

    /** @return codigo estavel de erro (ver {@code docs/api/erros.md}) */
    public String codigo() {
        return codigo;
    }

    /** @return mensagem para a pessoa, sem detalhe tecnico */
    public String mensagem() {
        return mensagem;
    }

    /**
     * Monta o erro padrao do projeto para este motivo.
     *
     * @return excecao com status, codigo, mensagem e, quando houver, o detalhe por campo
     */
    public ExcecaoNegocio excecao() {
        if (campo == null) {
            return new ExcecaoNegocio(status, codigo, mensagem);
        }
        return new ExcecaoNegocio(status, codigo, "O arquivo enviado não foi aceito.",
                List.of(new ErroPadrao.Detalhe(campo, mensagem)));
    }
}
