package br.com.regulacheck.indicador;

import br.com.regulacheck.indicador.repository.ContagensIndicadores;
import br.com.regulacheck.indicador.repository.IndicadorRepository;

/**
 * Repositorio de indicadores para testes: devolve as contagens definidas pelo teste (o SQL agregado real esta em
 * {@code IndicadorRepositoryJdbc} e so e exercitado contra o Oracle). Conta as consultas, para provar que o 403 vem
 * antes de qualquer acesso, e pode falhar na proxima consulta.
 */
public final class IndicadorRepositoryMemoria implements IndicadorRepository {

    private ContagensIndicadores contagens = ContagensIndicadores.vazias();
    private RuntimeException proximaFalha;
    private int consultas;

    @Override
    public synchronized ContagensIndicadores contar() {
        consultas++;
        if (proximaFalha != null) {
            RuntimeException falha = proximaFalha;
            proximaFalha = null;
            throw falha;
        }
        return contagens;
    }

    /** @param novas contagens que as proximas consultas devolvem */
    public synchronized void definir(ContagensIndicadores novas) {
        this.contagens = novas;
    }

    /** @param falha excecao que a proxima consulta lanca */
    public synchronized void falharNaProximaConsulta(RuntimeException falha) {
        this.proximaFalha = falha;
    }

    /** @return quantas vezes {@link #contar()} foi chamado */
    public synchronized int consultas() {
        return consultas;
    }
}
