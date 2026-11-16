package br.com.regulacheck.resumo.service;

import br.com.regulacheck.analise.entity.Analise;
import java.util.LinkedHashMap;
import java.util.Map;

/** Valida a saida estruturada do adaptador antes de persistir qualquer texto. */
public final class ValidadorResumo {

    private ValidadorResumo() {
    }

    /** Texto fechado por resultado, que nao pode introduzir exigencias ou promessas. */
    public static String textoEsperado(Analise analise) {
        return switch (analise.resultadoGeral()) {
            case COM_PENDENCIAS -> "Esta análise identificou pendências documentais. Confira as ações por regra antes "
                    + "de reenviar os documentos. A avaliação usa um protocolo acadêmico fictício.";
            case REQUER_CONFERENCIA -> "Alguns critérios não puderam ser verificados. Confira a transcrição dos "
                    + "documentos e peça uma nova análise. Um campo não localizado não comprova ausência no original.";
            case SEM_PENDENCIAS_IDENTIFICADAS -> "Nenhuma pendência foi identificada nesta revisão. Isso não "
                    + "significa aprovação do SUS. Os critérios são demonstrativos.";
        };
    }

    /** Exige correspondencia exata entre acoes propostas e pendencias deterministicas. */
    public static boolean valida(Analise analise, GeradorResumo.Saida saida) {
        if (saida == null || !textoEsperado(analise).equals(saida.texto()) || saida.texto().length() > 2000
                || saida.acoes() == null) {
            return false;
        }
        Map<String, String> esperadas = new LinkedHashMap<>();
        analise.pendencias().forEach(p -> esperadas.put(p.codigoRegra(), p.explicacao()));
        return esperadas.equals(saida.acoes());
    }
}
