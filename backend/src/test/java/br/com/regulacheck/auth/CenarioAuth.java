package br.com.regulacheck.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.auth.service.VerificadorSenha;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.documento.DocumentoRepositoryMemoria;
import br.com.regulacheck.extracao.ExtracaoRepositoryMemoria;
import br.com.regulacheck.integracao.extracao.LeitorDeDocumento;
import br.com.regulacheck.integracao.extracao.LeitorSimulado;
import br.com.regulacheck.protocolo.ProtocoloRepositoryMemoria;
import br.com.regulacheck.solicitacao.SolicitacaoRepositoryMemoria;
import br.com.regulacheck.usuario.entity.Perfil;
import br.com.regulacheck.usuario.entity.Usuario;
import java.time.Duration;
import java.time.Instant;

/**
 * Cenario compartilhado dos testes de autenticacao: tres contas ficticias (duas ativas e uma
 * desativada), repositorios em memoria e relogio controlavel. Os hashes usam custo 4 so para o teste
 * ser rapido (o custo 12 real e coberto por {@code HashesRc03CompatibilidadeTest}).
 */
public final class CenarioAuth {

    /** Senha fictícia usada apenas nestes testes. */
    public static final String SENHA = "Senha#Ficticia-2026";
    public static final String EMAIL_A = "paciente.a@exemplo.invalid";
    public static final String EMAIL_B = "paciente.b@exemplo.invalid";
    public static final String EMAIL_INATIVA = "inativa@exemplo.invalid";
    public static final String EMAIL_ADMIN = "admin@exemplo.invalid";
    public static final Duration TTL = Duration.ofMinutes(30);

    public final UsuarioRepositoryMemoria usuarios = new UsuarioRepositoryMemoria();
    public final SessaoRepositoryMemoria sessoes = new SessaoRepositoryMemoria();
    public final ProtocoloRepositoryMemoria protocolos = new ProtocoloRepositoryMemoria();
    public final SolicitacaoRepositoryMemoria solicitacoes = new SolicitacaoRepositoryMemoria();
    public final DocumentoRepositoryMemoria documentos = new DocumentoRepositoryMemoria(solicitacoes);
    public final ExtracaoRepositoryMemoria extracoes = new ExtracaoRepositoryMemoria(solicitacoes, documentos);
    public final RelogioFixo relogio = new RelogioFixo(Instant.parse("2026-11-12T20:00:00Z"));
    public final ContadorDeVerificacoes verificador = new ContadorDeVerificacoes();
    public final GeradorToken gerador = new GeradorToken();
    public final AutenticacaoService servico;

    /** Verificador que conta quantas vezes a verificacao "falsa" (igualdade de tempo) foi usada. */
    public static final class ContadorDeVerificacoes extends VerificadorSenha {
        public int contraHashFalso;
        public int reais;

        @Override
        public boolean verificar(String senha, String hash) {
            reais++;
            return super.verificar(senha, hash);
        }

        @Override
        public boolean verificarContraHashFalso(String senha) {
            contraHashFalso++;
            return super.verificarContraHashFalso(senha);
        }
    }

    public CenarioAuth() {
        String hash = new String(BCrypt.withDefaults().hash(4, SENHA.toCharArray()));
        usuarios.adicionar(new Usuario(1, EMAIL_A, "Paciente A (ficticia)", hash, Perfil.PACIENTE, true));
        usuarios.adicionar(new Usuario(2, EMAIL_B, "Paciente B (ficticia)", hash, Perfil.PACIENTE, true));
        usuarios.adicionar(new Usuario(3, EMAIL_INATIVA, "Conta Inativa", hash, Perfil.PACIENTE, false));
        usuarios.adicionar(new Usuario(4, EMAIL_ADMIN, "Administrador (ficticio)", hash, Perfil.ADMINISTRADOR, true));
        protocolos.adicionar(ProtocoloRepositoryMemoria.padrao(), true);
        this.servico = new AutenticacaoService(usuarios, sessoes, verificador, gerador, relogio, TTL);
    }

    /** @return as dependencias para montar o servidor com este cenario */
    public Dependencias dependencias() {
        return dependencias(new LeitorSimulado());
    }

    /**
     * @param leitor leitor de documentos a usar (o simulado de producao, um simulado com catalogo de teste ou um
     *     leitor falso)
     * @return as dependencias para montar o servidor com este cenario e esse leitor
     */
    public Dependencias dependencias(LeitorDeDocumento leitor) {
        return new Dependencias(usuarios, sessoes, protocolos, solicitacoes, documentos, extracoes, leitor, relogio);
    }
}
