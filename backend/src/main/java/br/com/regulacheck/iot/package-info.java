/**
 * Modulo iot: estacao de captura simulada (RC-21 + RC-22, Epico 7).
 *
 * <p>A paciente abre a sessao de captura do proprio pedido; o simulador ({@code simulador-iot/}) envia leituras de
 * luminosidade (lux) e distancia (cm) com a credencial restrita {@code X-Chave-Simulador}; a paciente consulta a
 * telemetria com a situacao de cada sensor e uma orientacao. Os sensores sao simulados, os limites sao
 * demonstrativos (sem calibracao) e as leituras nao comprovam a legibilidade do arquivo nem bloqueiam nenhum fluxo.
 */
package br.com.regulacheck.iot;
