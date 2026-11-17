/**
 * Modulo reenvio (RC-16): verificacao da versao corrigida do pedido. Orquestra, para a revisao atual, a leitura
 * simulada (RC-10), a conferencia da paciente (RC-11), a analise (RC-13) e o resumo simulado (RC-14), sem regra
 * propria: cada etapa usa o caso de uso da sua historia, com as mesmas garantias. Nao chama IA real nem le documento
 * de verdade; leitura e resumo sao simulados.
 */
package br.com.regulacheck.reenvio;
