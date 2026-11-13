package br.com.regulacheck.protocolo.controller;

import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.protocolo.dto.ListaProtocolosResponse;
import br.com.regulacheck.protocolo.service.ProtocoloService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code GET /protocolos}: lista os servicos (protocolos academicos ficticios) que a paciente pode escolher. Rota
 * privada de {@code PACIENTE}; a autenticacao e o perfil sao conferidos antes por {@code RotasPrivadas}.
 */
public class ProtocoloController extends ServletJson {

    private static final long serialVersionUID = 1L;

    private final transient ProtocoloService servico;

    /** @param servico casos de uso do catalogo */
    public ProtocoloController(ProtocoloService servico) {
        super("GET");
        this.servico = servico;
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        responderJson(resposta, HttpServletResponse.SC_OK, ListaProtocolosResponse.de(servico.listarAtivos()));
    }
}
