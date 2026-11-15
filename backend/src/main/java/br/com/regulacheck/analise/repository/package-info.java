/**
 * Modulo analise, camada repository (RC-13): persistencia das analises, do historico e da analise em andamento
 * ({@code PROCESSANDO} com instante de inicio), por JDBC com SQL parametrizado e transacoes curtas sob o bloqueio
 * da linha do pedido.
 */
package br.com.regulacheck.analise.repository;
