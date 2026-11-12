package br.com.regulacheck.auth.service;

import br.com.regulacheck.auth.entity.Sessao;
import br.com.regulacheck.auth.repository.SessaoRepository;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.usuario.entity.Perfil;
import br.com.regulacheck.usuario.entity.Usuario;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Casos de uso de autenticacao: entrar, validar a sessao de uma requisicao e sair.
 *
 * <p>Esta classe nao guarda dados de requisicao em campos (e compartilhada entre requisicoes). Nenhum
 * log aqui contem senha, hash, token ou e-mail: so resultado, codigo, identificador tecnico da conta e
 * duracao.
 */
public class AutenticacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(AutenticacaoService.class);

    private static final String MENSAGEM_CREDENCIAIS = "E-mail ou senha inválidos.";
    private static final String MENSAGEM_SESSAO = "Sessão ausente ou expirada. Entre novamente.";

    /**
     * Resultado de um login bem-sucedido. O {@code token} so existe aqui: depois disso so o hash dele
     * fica guardado.
     *
     * @param token credencial da sessao, para ser enviada em {@code Authorization: Bearer}
     * @param expiraEm instante em que a sessao deixa de valer
     * @param nome nome de exibicao da conta
     * @param perfil perfil da conta
     */
    public record SessaoIniciada(String token, Instant expiraEm, String nome, Perfil perfil) {

        /** Oculta o token para que nao vaze em logs. */
        @Override
        public String toString() {
            return "SessaoIniciada[expiraEm=" + expiraEm + ", perfil=" + perfil + "]";
        }
    }

    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final VerificadorSenha verificador;
    private final GeradorToken gerador;
    private final Clock relogio;
    private final Duration validade;

    /**
     * @param usuarios consulta de contas
     * @param sessoes persistencia das sessoes
     * @param verificador conferencia de senha (BCrypt)
     * @param gerador geracao do token e do seu hash
     * @param relogio relogio da aplicacao (substituivel em testes)
     * @param validade tempo de vida de cada sessao, contado a partir do login
     */
    public AutenticacaoService(UsuarioRepository usuarios, SessaoRepository sessoes, VerificadorSenha verificador,
            GeradorToken gerador, Clock relogio, Duration validade) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.verificador = verificador;
        this.gerador = gerador;
        this.relogio = relogio;
        this.validade = validade;
    }

    /**
     * Autentica uma conta e abre uma sessao nova e independente das demais da mesma conta.
     *
     * <p>Senha errada, e-mail inexistente, conta desativada e senha acima do limite do BCrypt geram o
     * <strong>mesmo</strong> erro, para nao revelar quais contas existem. Quando o e-mail nao existe,
     * gasta-se o mesmo tempo de uma verificacao real. O token so e devolvido depois de a sessao ser
     * gravada.
     *
     * @param email e-mail informado (aparado e comparado em minusculas)
     * @param senha senha informada, sem alteracao
     * @return sessao aberta, com o token
     * @throws ExcecaoNegocio 401 {@code CREDENCIAIS_INVALIDAS} se as credenciais nao conferem; 503 se o
     *     banco estiver indisponivel (nenhuma sessao e aberta)
     */
    public SessaoIniciada login(String email, String senha) {
        long inicio = System.nanoTime();
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Optional<Usuario> encontrado = usuarios.buscarPorEmail(emailNormalizado);
        if (encontrado.isEmpty()) {
            verificador.verificarContraHashFalso(senha);
            throw falhaDeCredencial(null, inicio);
        }
        Usuario usuario = encontrado.get();
        // A senha e conferida mesmo para conta desativada, para o tempo nao distinguir os casos.
        boolean confere = verificador.verificar(senha, usuario.senhaHash());
        if (!confere || !usuario.ativo()) {
            throw falhaDeCredencial(usuario, inicio);
        }

        String token = gerador.novo();
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validade);
        sessoes.criar(usuario.id(), gerador.hash(token), agora, expiraEm);

        LOG.info("Login resultado=SUCESSO usuarioId={} duracaoMs={}", usuario.id(), milissegundos(inicio));
        return new SessaoIniciada(token, expiraEm, usuario.nome(), usuario.perfil());
    }

    /**
     * Valida o token de uma requisicao.
     *
     * <p>Uma sessao vale enquanto nao foi encerrada, nao expirou e a conta continua ativa. O motivo exato
     * da recusa nunca e informado a quem chamou.
     *
     * @param token token recebido em {@code Authorization: Bearer}; pode ser {@code null}
     * @return identidade da sessao
     * @throws ExcecaoNegocio 401 {@code NAO_AUTENTICADO} se a sessao nao for valida; 503 se o banco
     *     estiver indisponivel
     */
    public SessaoAutenticada autenticar(String token) {
        if (!gerador.formatoValido(token)) {
            throw naoAutenticado();
        }
        Sessao sessao = sessoes.buscarPorTokenHash(gerador.hash(token))
                .filter(s -> s.aberta(relogio.instant()))
                .orElseThrow(AutenticacaoService::naoAutenticado);
        Usuario usuario = usuarios.buscarPorId(sessao.usuarioId())
                .filter(Usuario::ativo)
                .orElseThrow(AutenticacaoService::naoAutenticado);
        return new SessaoAutenticada(sessao.id(), usuario.id(), usuario.nome(), usuario.perfil(), sessao.expiraEm());
    }

    /**
     * Encerra a sessao informada; as demais sessoes da mesma conta continuam validas. Repetir a chamada
     * nao tem efeito adicional.
     *
     * @param sessao sessao validada por {@link #autenticar(String)}
     * @throws ExcecaoNegocio 503 se o banco estiver indisponivel
     */
    public void encerrar(SessaoAutenticada sessao) {
        sessoes.encerrar(sessao.sessaoId(), relogio.instant());
        LOG.info("Logout resultado=SUCESSO usuarioId={}", sessao.usuarioId());
    }

    private ExcecaoNegocio falhaDeCredencial(Usuario usuario, long inicio) {
        LOG.info("Login resultado=FALHA codigo=CREDENCIAIS_INVALIDAS usuarioId={} duracaoMs={}",
                usuario == null ? "desconhecido" : String.valueOf(usuario.id()), milissegundos(inicio));
        return new ExcecaoNegocio(401, "CREDENCIAIS_INVALIDAS", MENSAGEM_CREDENCIAIS);
    }

    /** @return 401 {@code NAO_AUTENTICADO} com a mensagem unica de sessao invalida (mesma para qualquer motivo) */
    public static ExcecaoNegocio naoAutenticado() {
        return new ExcecaoNegocio(401, "NAO_AUTENTICADO", MENSAGEM_SESSAO);
    }

    private static long milissegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
