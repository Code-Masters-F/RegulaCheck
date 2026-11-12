# Modelo conceitual de dados — RC-02

Conceitual; a modelagem relacional (Oracle) é a RC-03. Nomes em português sem acentos.

## Solicitacao
- `id`, `protocoloId`, `versaoProtocolo`, `estado` (ver estados), `revisao` (inteiro ≥ 1), `criadaEm`, `atualizadaEm` (UTC).
- A dona é a paciente autenticada (nunca informada no corpo).
- Relaciona 0..3 **Documentos** por revisão e 0..n **Análises**.

## Documento
- `id`, `solicitacaoId`, `nomeOriginal`, `tipoReal` (PDF/JPEG/PNG), `tamanhoBytes`, `paginas`, `revisaoEnvio`, `enviadoEm`.
- Limites: até 3 arquivos, 5 MB cada, PDF até 5 páginas.

## Analise
- `id`, `solicitacaoId`, `revisao`, `versaoProtocolo`, `resultadoGeral` (COM_PENDENCIAS, REQUER_CONFERENCIA, SEM_PENDENCIAS_IDENTIFICADAS), `simulado` (booleano, sempre verdadeiro para resumo/extração mockados), `vigente` (booleano), `criadaEm`.
- Contém 1 **ResultadoRegra** por regra do protocolo e 0..n **Pendências**.

## ResultadoRegra
- `codigoRegra` (RC-R01…RC-R06), `resultado` (ATENDE, PENDENTE, NAO_VERIFICAVEL, NAO_APLICAVEL).

## Pendencia
- `codigoRegra`, `mensagem`, `responsavelCorrecao` (PACIENTE, UNIDADE_EMISSORA), `resultado` (PENDENTE ou NAO_VERIFICAVEL).

## Erro
- `codigo`, `mensagem`, `detalhes[]` (`campo`, `mensagem`), `idRastreamento` (opcional). Sem stack trace, SQL ou segredos.

## Estado e transição
- Estados: RASCUNHO, PROCESSANDO, REQUER_CONFERENCIA, COM_PENDENCIAS, SEM_PENDENCIAS_IDENTIFICADAS, FALHA_ANALISE, SUBMETIDA_SIMULACAO. Transições em `contracts/estados-e-revisoes.md`.

## Regras de validação (resumo)
- `revisao` só aumenta; resultado de análise só é `vigente` se `revisao` da análise = `revisao` atual da solicitação.
- Documento: tipo real verificado, tamanho e páginas dentro dos limites.
- Submissão: só a partir de SEM_PENDENCIAS_IDENTIFICADAS; repetir a operação não cria outra submissão.
