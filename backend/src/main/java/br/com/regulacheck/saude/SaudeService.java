package br.com.regulacheck.saude;

/**
 * Caso de uso do health tecnico: consulta o repositorio e devolve o estado.
 *
 * <p>Sem estado por requisicao; as dependencias chegam pelo construtor.
 */
public class SaudeService {

    private final SaudeRepository repositorio;

    /**
     * @param repositorio origem do estado tecnico
     */
    public SaudeService(SaudeRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * @return resposta do health com apenas o estado tecnico (sem detalhes internos)
     */
    public SaudeResposta consultar() {
        return new SaudeResposta(repositorio.estadoTecnico());
    }
}
