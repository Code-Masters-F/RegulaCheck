package br.com.regulacheck.resumo.service;

import br.com.regulacheck.analise.entity.Analise;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adaptador simulado: usa somente o resultado e as mensagens fechadas das regras gravadas. */
public final class GeradorResumoMockado implements GeradorResumo {

    @Override
    public Saida gerar(Analise analise) {
        Map<String, String> acoes = new LinkedHashMap<>();
        analise.pendencias().forEach(p -> acoes.put(p.codigoRegra(), p.explicacao()));
        return new Saida(ValidadorResumo.textoEsperado(analise), acoes);
    }
}
