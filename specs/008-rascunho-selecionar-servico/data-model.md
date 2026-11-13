# Data Model: Criar rascunho e selecionar serviço (RC-07)

**Uma mudança de esquema, aditiva:** a restrição `RC_REVISAO_CK_MOTIVO` passa a aceitar `PROTOCOLO_ALTERADO`. Nenhuma tabela, coluna ou linha nova de esquema; nenhum dado é apagado. Tabelas usadas: `RC_PROTOCOLO`, `RC_SOLICITACAO`, `RC_REVISAO` (RC-03) e `RC_USUARIO` (dono, por chave estrangeira).

## Correspondência com o banco

| Conceito da spec | Tabela / coluna | Observação |
|---|---|---|
| Serviço (protocolo acadêmico) | `RC_PROTOCOLO` (`ID`, `IDENTIFICADOR`, `VERSAO`, `LOCALIDADE`, `ESPECIALIDADE`, `AVISO_FICTICIO`, `ATIVO`) | só linhas com `ATIVO = 1` aparecem no catálogo e podem ser escolhidas; `protocoloId` público = `IDENTIFICADOR-VERSAO` |
| Solicitação (pedido) | `RC_SOLICITACAO` (`ID`, `PACIENTE_ID`, `PROTOCOLO_ID`, `ESTADO`, `REVISAO_ATUAL`, `CRIADA_EM`, `ATUALIZADA_EM`) | nesta história `ESTADO` só vale `RASCUNHO`; `PACIENTE_ID` vem da sessão |
| Revisão | `RC_REVISAO` (`SOLICITACAO_ID`, `NUMERO`, `MOTIVO`, `CRIADA_EM`) | PK composta; revisão 1 = `CRIACAO`; troca de serviço = `PROTOCOLO_ALTERADO` |
| Paciente | `RC_USUARIO` (`ID`, `PERFIL = 'PACIENTE'`) | identificada pela sessão (RC-04) |

## Script de banco (novo)

`database/scripts/06_motivo_revisao_protocolo.sql` — depois do `01_`; seguro de repetir só até o `ADD` (ver o cabeçalho do arquivo):

```sql
ALTER TABLE RC_REVISAO DROP CONSTRAINT RC_REVISAO_CK_MOTIVO;
ALTER TABLE RC_REVISAO ADD CONSTRAINT RC_REVISAO_CK_MOTIVO CHECK (MOTIVO IN
    ('CRIACAO', 'DOCUMENTO_ENVIADO', 'DOCUMENTO_SUBSTITUIDO', 'DOCUMENTO_REMOVIDO',
     'CONFERENCIA_ALTERADA', 'PROTOCOLO_ALTERADO'));
```

Efeito: aditivo, só amplia o conjunto aceito; linhas existentes continuam válidas (a verificação reavalia todas, e todas usam valores que seguem na lista). O `01_criar_esquema.sql` recebe a mesma lista para instalações novas. O `90_limpeza_DESTRUTIVA.sql` não muda (apaga a tabela inteira).

## Tipos de código (sem ORM)

| Tipo | Pacote | Papel |
|---|---|---|
| `Protocolo` (record) | `protocolo.entity` | linha de `RC_PROTOCOLO` já lida; expõe `protocoloId()` = `identificador + "-" + versao` |
| `EstadoSolicitacao` (enum) | `solicitacao.entity` | os 7 estados da `RC_SOLICITACAO_CK_ESTADO`; nesta história só `RASCUNHO` é produzido |
| `MotivoRevisao` (enum) | `solicitacao.entity` | os 6 motivos da `RC_REVISAO_CK_MOTIVO` |
| `Solicitacao` (record) | `solicitacao.entity` | `id`, `pacienteId`, `protocolo` (`Protocolo`), `estado`, `revisaoAtual`, `criadaEm`, `atualizadaEm` (instantes UTC) |
| `ResultadoTroca` (record + enum `Situacao`) | `solicitacao.repository` | `NAO_ENCONTRADA`, `FORA_DE_RASCUNHO`, `SEM_MUDANCA`, `ALTERADA` + `Solicitacao` quando existe |

Entidades nunca saem pela API; o mapeamento para DTO é explícito.

## DTOs (contrato público)

**Entrada** `SolicitacaoRequest` — usado em `POST` e `PUT`:

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `protocoloId` | string | sim | não vazio, ≤ 100 caracteres; deve ser de um protocolo ativo. Campos desconhecidos (`pacienteId`, `estado`, `revisao`…) são ignorados e nunca lidos |

**Saída** `SolicitacaoResponse` (criação 201, reabrir 200, troca 200, itens da lista):

| Campo | Tipo | Observação |
|---|---|---|
| `id` | string | decimal opaco (ex.: `"12"`) |
| `protocoloId` | string | `IDENTIFICADOR-VERSAO` |
| `servico` | objeto | `{ localidade, especialidade, avisoFicticio }` do protocolo |
| `estado` | string | nesta história `"RASCUNHO"` |
| `revisao` | inteiro ≥ 1 | revisão vigente |
| `criadaEm`, `atualizadaEm` | string ISO-8601 UTC | |
| `simulacao` | booleano | sempre `true` |

**Saída** `ProtocoloResponse` (item do catálogo): `protocoloId`, `identificador`, `versao`, `localidade`, `especialidade`, `avisoFicticio`, `simulacao: true`.

Listas: `{ "itens": [...] }` (catálogo) e `{ "itens": [...], "limite": 50 }` (pedidos). Nada de `pacienteId`, `ID` do protocolo, nem campos internos aparece em resposta.

## Invariantes e transições

- Criar: `(nenhum) → RASCUNHO`, `REVISAO_ATUAL = 1`, uma linha em `RC_REVISAO` (1, `CRIACAO`) — ambas gravadas na mesma transação.
- Trocar serviço: só em `RASCUNHO`; mudou → `REVISAO_ATUAL + 1`, `ATUALIZADA_EM = agora`, nova linha `RC_REVISAO(n+1, PROTOCOLO_ALTERADO)`; não mudou → nada muda.
- `RC_REVISAO.NUMERO` por pedido é sequencial sem lacunas (consequência do bloqueio de linha) e único (PK).
- Pedido de uma paciente nunca é visível a outra: toda leitura e escrita filtra por `PACIENTE_ID` na própria instrução SQL.
- `ESTADO` diferente de `RASCUNHO` nunca é alterado por esta história (as transições seguintes são de outras histórias).
