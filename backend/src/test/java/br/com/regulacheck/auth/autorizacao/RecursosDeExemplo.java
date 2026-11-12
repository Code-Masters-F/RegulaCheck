package br.com.regulacheck.auth.autorizacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Recursos ficticios, so de teste, para provar a regra de dona sem criar CRUD de negocio.
 *
 * <p>Para cada um dos cinco tipos ha um recurso da paciente A (conta 1), um da paciente B (conta 2) e
 * um de uma conta desativada (conta 3). Identificadores: {@code (ordem do tipo + 1) * 100 + 1/2/3}.
 * O documento pertence a uma solicitacao (o pai tem o mesmo indice dentro do tipo SOLICITACAO).
 * A busca recebe a dona e devolve o resultado ja filtrado (padrao D5 do plano), como o Repository
 * real fara com a juncao ate {@code RC_SOLICITACAO.PACIENTE_ID}.
 */
public final class RecursosDeExemplo {

    /** Tipos de recurso de paciente protegidos pela regra de dona. */
    public enum Tipo {
        SOLICITACAO("solicitacoes"), DOCUMENTO("documentos"), ANALISE("analises"), RESUMO("resumos"),
        SUBMISSAO("submissoes");

        private final String caminho;

        Tipo(String caminho) {
            this.caminho = caminho;
        }

        /** @return segmento de caminho da rota de exemplo deste tipo */
        public String caminho() {
            return caminho;
        }

        /** @param indice 1 = paciente A, 2 = paciente B, 3 = conta desativada; @return identificador */
        public long id(int indice) {
            return (ordinal() + 1) * 100L + indice;
        }
    }

    /** Estado mutavel de um recurso de exemplo. */
    public static final class Recurso {
        private final Tipo tipo;
        private final long id;
        private final long dona;
        private final long pai;
        private int versao;
        private boolean removido;

        Recurso(Tipo tipo, long id, long dona, long pai) {
            this.tipo = tipo;
            this.id = id;
            this.dona = dona;
            this.pai = pai;
        }

        public Tipo tipo() {
            return tipo;
        }

        public long id() {
            return id;
        }

        public int versao() {
            return versao;
        }

        public boolean removido() {
            return removido;
        }
    }

    private static final long TELEMETRIA_ABERTA = 901;
    private static final long TELEMETRIA_ENCERRADA = 902;

    private final List<Recurso> recursos = new ArrayList<>();
    private int acessos;

    /** Cria os 15 recursos de exemplo (5 tipos x 3 contas). */
    public RecursosDeExemplo() {
        for (Tipo tipo : Tipo.values()) {
            for (int indice = 1; indice <= 3; indice++) {
                long pai = tipo == Tipo.DOCUMENTO ? Tipo.SOLICITACAO.id(indice) : 0;
                recursos.add(new Recurso(tipo, tipo.id(indice), indice, pai));
            }
        }
    }

    /**
     * Busca ja filtrada pela dona.
     *
     * @param tipo tipo do recurso
     * @param id identificador
     * @param pai solicitacao informada na rota (so para documento); {@code null} nos demais
     * @param donaDaSessao conta autenticada
     * @return o recurso, se existir, nao tiver sido removido e for da conta
     */
    public synchronized Optional<Recurso> buscar(Tipo tipo, long id, Long pai, long donaDaSessao) {
        acessos++;
        return recursos.stream()
                .filter(r -> r.tipo == tipo && r.id == id && !r.removido && r.dona == donaDaSessao)
                .filter(r -> tipo != Tipo.DOCUMENTO || (pai != null && r.pai == pai))
                .findFirst();
    }

    /** @return identificadores dos recursos da conta, para a listagem */
    public synchronized List<Long> listar(Tipo tipo, long donaDaSessao) {
        acessos++;
        return recursos.stream().filter(r -> r.tipo == tipo && !r.removido && r.dona == donaDaSessao)
                .map(r -> r.id).toList();
    }

    /** Altera o recurso (conta uma nova versao). */
    public synchronized void alterar(Recurso recurso) {
        recurso.versao++;
    }

    /** Remove o recurso. */
    public synchronized void remover(Recurso recurso) {
        recurso.removido = true;
    }

    /** @return quantas consultas ao repositorio ocorreram (prova que o 403 nao chega a consultar) */
    public synchronized int acessos() {
        return acessos;
    }

    /** @return estado atual do recurso, para conferir que nada foi alterado, sem contar como acesso */
    public synchronized Recurso estado(Tipo tipo, int indice) {
        return recursos.stream().filter(r -> r.tipo == tipo && r.id == tipo.id(indice)).findFirst().orElseThrow();
    }

    /** @return totais agregados, sem identificar ninguem */
    public synchronized java.util.Map<String, Integer> totais() {
        java.util.Map<String, Integer> totais = new java.util.LinkedHashMap<>();
        for (Tipo tipo : Tipo.values()) {
            totais.put("total" + tipo.name().charAt(0) + tipo.name().substring(1).toLowerCase(),
                    (int) recursos.stream().filter(r -> r.tipo == tipo && !r.removido).count());
        }
        return totais;
    }

    /** @return {@code true} se a sessao de captura de exemplo existe e esta aberta */
    public boolean capturaAberta(long id) {
        return Set.of(TELEMETRIA_ABERTA).contains(id);
    }

    /** @return identificador da sessao de captura aberta */
    public static long capturaAbertaId() {
        return TELEMETRIA_ABERTA;
    }

    /** @return identificador da sessao de captura encerrada */
    public static long capturaEncerradaId() {
        return TELEMETRIA_ENCERRADA;
    }
}
