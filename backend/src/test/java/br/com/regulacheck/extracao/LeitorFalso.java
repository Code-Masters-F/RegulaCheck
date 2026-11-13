package br.com.regulacheck.extracao;

import br.com.regulacheck.integracao.extracao.ArquivoParaLeitura;
import br.com.regulacheck.integracao.extracao.LeituraDeDocumento;
import br.com.regulacheck.integracao.extracao.LeituraIndisponivelException;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Leitor de teste configuravel: devolve o que o teste mandar, falha ou devolve saida fora do contrato, e registra
 * quantas vezes foi chamado e com quais bytes. Serve para tratar casos que o leitor simulado nao produz
 * (protegido, falha, saida invalida).
 */
public final class LeitorFalso implements LeitorDeDocumento {

    private final Function<ArquivoParaLeitura, LeituraDeDocumento> comportamento;
    private final List<byte[]> recebidos = new ArrayList<>();
    private int chamadas;

    /** @param comportamento o que devolver (ou lancar) para cada arquivo */
    public LeitorFalso(Function<ArquivoParaLeitura, LeituraDeDocumento> comportamento) {
        this.comportamento = comportamento;
    }

    /** @return leitor que sempre devolve leitura vazia */
    public static LeitorFalso vazio() {
        return new LeitorFalso(arquivo -> LeituraDeDocumento.vazia());
    }

    /** @return leitor que sempre devolve arquivo protegido */
    public static LeitorFalso protegido() {
        return new LeitorFalso(arquivo -> LeituraDeDocumento.protegido());
    }

    /** @return leitor que sempre falha (indisponivel) */
    public static LeitorFalso queFalha() {
        return new LeitorFalso(arquivo -> falhar());
    }

    /**
     * Para uso dentro do comportamento configurado: faz esta chamada do leitor falhar (indisponivel).
     *
     * @return nunca retorna
     */
    public static LeituraDeDocumento falhar() {
        throw new FalhaDoLeitor();
    }

    @Override
    public synchronized LeituraDeDocumento ler(ArquivoParaLeitura arquivo) throws LeituraIndisponivelException {
        chamadas++;
        recebidos.add(arquivo.conteudo());
        try {
            return comportamento.apply(arquivo);
        } catch (FalhaDoLeitor falha) {
            throw new LeituraIndisponivelException("Leitor indisponivel (simulado no teste).");
        }
    }

    /** @return quantas vezes o leitor foi chamado */
    public synchronized int chamadas() {
        return chamadas;
    }

    /** @return os bytes recebidos em cada chamada */
    public synchronized List<byte[]> recebidos() {
        return List.copyOf(recebidos);
    }

    /** Sinal interno de falha do leitor falso. */
    private static final class FalhaDoLeitor extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
