package br.com.regulacheck.usuario.entity;

/**
 * Perfil de uma conta. Conjunto fechado, igual ao CHECK {@code RC_USUARIO_CK_PERFIL} do banco.
 *
 * <p>As regras de acesso por perfil (RC-05) sao declaradas por rota em {@code RotasPrivadas} e conferidas
 * por {@code AutorizacaoFiltro}. O simulador de sensores nao e um perfil de conta: usa a chave
 * {@code IOT_API_KEY} (ver {@code SimuladorFiltro}).
 */
public enum Perfil {
    /** Conta da paciente, usuaria principal do MVP. */
    PACIENTE,
    /** Conta administrativa; nao ganha acesso irrestrito aos documentos das pacientes. */
    ADMINISTRADOR
}
