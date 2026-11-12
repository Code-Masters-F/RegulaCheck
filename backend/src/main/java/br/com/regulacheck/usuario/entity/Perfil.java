package br.com.regulacheck.usuario.entity;

/**
 * Perfil de uma conta. Conjunto fechado, igual ao CHECK {@code RC_USUARIO_CK_PERFIL} do banco.
 *
 * <p>Nesta etapa o perfil so e informado a quem entra; as regras de acesso por perfil sao da RC-05.
 */
public enum Perfil {
    /** Conta da paciente, usuaria principal do MVP. */
    PACIENTE,
    /** Conta administrativa; nao ganha acesso irrestrito aos documentos das pacientes. */
    ADMINISTRADOR
}
