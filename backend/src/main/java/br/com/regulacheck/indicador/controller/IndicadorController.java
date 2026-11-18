package br.com.regulacheck.indicador.controller;

import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.indicador.dto.IndicadoresResponse;
import br.com.regulacheck.indicador.service.IndicadorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code GET /admin/indicadores}: protocolo ativo e indicadores agregados (RC-20). Rota privada so de
 * {@code ADMINISTRADOR}: autenticacao (401) e perfil (403) sao conferidos antes por {@code RotasPrivadas}, entao a
 * paciente nunca chega a este controller nem ao banco.
 */
public class IndicadorController extends ServletJson {

    private static final long serialVersionUID = 1L;

    private final transient IndicadorService servico;

    /** @param servico caso de uso dos indicadores */
    public IndicadorController(IndicadorService servico) {
        super("GET");
        this.servico = servico;
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        responderJson(resposta, HttpServletResponse.SC_OK, IndicadoresResponse.de(servico.consultar()));
    }
}
