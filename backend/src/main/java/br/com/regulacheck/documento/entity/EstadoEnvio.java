package br.com.regulacheck.documento.entity;

/**
 * Estado do <strong>envio</strong> de um documento, nao da analise dele. Conjunto fechado: as historias de
 * processamento (extracao, conferencia) acrescentam valores novos sem mudar o significado dos existentes.
 *
 * <p>Hoje so existe um valor, porque um arquivo recusado ou nao registrado nunca vira documento; estados
 * transitorios como "enviando" ou "falhou" pertencem ao aplicativo enquanto ele espera a resposta.
 */
public enum EstadoEnvio {

    /** O arquivo foi conferido, guardado na area privada e registrado no pedido. Nao significa analisado. */
    ENVIADO
}
