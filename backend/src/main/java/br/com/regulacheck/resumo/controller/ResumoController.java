package br.com.regulacheck.resumo.controller;

import br.com.regulacheck.analise.dto.AnaliseResponse;
import br.com.regulacheck.auth.autorizacao.Erros;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.resumo.service.ResumoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/** POST /solicitacoes/{id}/resumo: orientacao simulada da analise vigente. */
public final class ResumoController extends ServletJson {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^[1-9][0-9]{0,17}$");
    private final transient ResumoService servico;

    /** @param servico caso de uso de resumo */
    public ResumoController(ResumoService servico) {
        super("POST");
        this.servico = servico;
    }

    /** Reconhece o subcaminho de resumo, deixando validacao completa para a rota. */
    public static boolean reconhece(String caminho) {
        if (caminho == null) {
            return false;
        }
        String[] partes = caminho.split("/", -1);
        return partes.length >= 3 && "resumo".equals(partes[2]);
    }

    /** Despacha pelo mesmo filtro privado de solicitacoes. */
    public void atender(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {
        service(requisicao, resposta);
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        String[] partes = requisicao.getPathInfo().split("/", -1);
        if (partes.length != 3 || !ID.matcher(partes[1]).matches()) {
            throw Erros.naoEncontrado();
        }
        var resultado = servico.gerar(AutenticacaoFiltro.sessaoDe(requisicao), Long.parseLong(partes[1]));
        responderJson(resposta, resultado.criada() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK,
                AnaliseResponse.de(resultado.analise()));
    }
}
