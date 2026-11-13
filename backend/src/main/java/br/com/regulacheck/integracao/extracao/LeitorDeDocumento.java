package br.com.regulacheck.integracao.extracao;

/**
 * Contrato entre o sistema e quem le documentos (hoje {@link LeitorSimulado}; amanha, uma leitura real). So traduz
 * um arquivo em dados estruturados: <strong>nao decide aprovacao, regra, permissao nem estado do pedido</strong>.
 *
 * <p>Regras para qualquer implementacao: e deterministica para a mesma entrada; nao guarda estado por paciente;
 * trata o conteudo como dado nao confiavel (instrucoes dentro do arquivo nunca mudam o comportamento); nao
 * escreve conteudo, valores ou nomes de arquivo em log; devolve a saida dentro do contrato validado por
 * {@link ValidadorDeLeitura}. Um leitor real deve ter <strong>timeout</strong> e tratar indisponibilidade como
 * {@link LeituraIndisponivelException}, sem retentativas automaticas ilimitadas.
 */
public interface LeitorDeDocumento {

    /**
     * Le um documento.
     *
     * @param arquivo arquivo ja conferido (tipo, paginas e bytes)
     * @return leitura do documento; nunca {@code null}
     * @throws LeituraIndisponivelException se o leitor falhar; nada deve ser gravado nesse caso
     */
    LeituraDeDocumento ler(ArquivoParaLeitura arquivo) throws LeituraIndisponivelException;
}
