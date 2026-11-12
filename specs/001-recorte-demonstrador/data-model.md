# Modelo conceitual — RC-01

Conceitual e independente de tecnologia; a modelagem relacional é RC-03.

## Protocolo
- `identificador` (ex.: PROTOCOLO-ACADEMICO-FICTICIO), `versao` (v0.1), `localidade`, `especialidade`, `aviso_ficticio` (texto fixo).
- Tem 1..6 **Regras**.

## Regra
- `codigo` (único, formato RC-Rnn), `descricao`, `evidencia_exigida`, `natureza` (OBRIGATORIA | CONDICIONAL), `condicao_aplicacao` (obrigatória quando CONDICIONAL), `mensagem` (para a paciente), `responsavel_correcao` (PACIENTE | UNIDADE_EMISSORA).
- Pertence a um Protocolo/versão.
- `responsavel_correcao` é único por regra. A mensagem para resultado `NAO_VERIFICAVEL` é padrão do protocolo (ver contrato), com a paciente como responsável.

## Resultado de regra (por análise)
- Valores: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL`.
- Derivação (aplicabilidade × evidência):

| Aplicável? | Evidência | Resultado |
|---|---|---|
| Sim | encontrada e conforme | ATENDE |
| Sim | não localizada em documento legível (ou divergente) | PENDENTE |
| Sim | ilegível, protegido ou leitura vazia | NAO_VERIFICAVEL |
| Não | qualquer | NAO_APLICAVEL |
| Aplicabilidade indeterminada (dado da condição ilegível) | qualquer | NAO_VERIFICAVEL |

## Resultado geral
- `COM_PENDENCIAS` se existe PENDENTE; senão `REQUER_CONFERENCIA` se existe NAO_VERIFICAVEL; senão `SEM_PENDENCIAS_IDENTIFICADAS`. NAO_APLICAVEL não influencia.
- Nomenclatura: `REQUER_CONFERENCIA` já existe em `docs/arquitetura.md`; a tarefa T009 decide se o significado é ampliado para "há regra não verificável" ou se outro estado é criado, e registra a decisão.
- Não é aprovação do SUS. `FALHA_ANALISE` (falha de processamento) é fora do catálogo.

## Registro de tecnologia / limitação
- `item`, `status` (ADOTADA | SIMULADA | DESCARTADA | PENDENTE | FORA_DO_ESCOPO), `observacao`.
