package br.com.regulacheck.resumo.service;

import br.com.regulacheck.analise.entity.Analise;
import java.util.Map;

/** Fronteira do gerador de orientacao; a mesma saida e validada para qualquer adaptador. */
public interface GeradorResumo {

    /** Texto geral e acoes por codigo de regra, sem conteudo documental livre. */
    record Saida(String texto, Map<String, String> acoes) {
    }

    /** Gera a proposta para uma analise ja verificada. */
    Saida gerar(Analise analise);
}
