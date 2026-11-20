# Roteiro de avaliação da API (RC-28)

Percurso único, do login ao comprovante da submissão simulada, para executar pela documentação interativa (`/docs`) ou por `curl`. **Dados sintéticos**, protocolo acadêmico fictício e leitura, resumo e submissão **simulados**: nada é enviado ao SUS e nenhum resultado é aprovação, diagnóstico ou vaga.

O mesmo percurso é executado automaticamente pelas rotas reais, com repositórios em memória, no `RoteiroAvaliacaoTest` (`backend/src/test/java/br/com/regulacheck/documentacao/`), que confere os status e os campos de cada passo. Com o Oracle real, a execução é manual (este roteiro).

## Antes de começar

1. Banco Oracle com os scripts e a carga sintética ([database/scripts/README.md](../../database/scripts/README.md)) e a API em execução ([docs/ambiente.md](../ambiente.md)). Sem `DB_*`, a página abre, mas as rotas de negócio respondem **503 `DEPENDENCIA_INDISPONIVEL`**.
2. Contas de demonstração: `paciente.a@exemplo.invalid` (paciente) e `admin@exemplo.invalid` (administrador). A senha de demonstração fictícia está em [database/scripts/README.md](../../database/scripts/README.md#contas-e-senhas-de-demonstração) e não é repetida aqui.
3. Amostras (na raiz de `samples/`, as mesmas do conjunto 01): `encaminhamento-sintetico.pdf` e `identificacao-sintetica.png`.

**Pela página:** abra `http://localhost:8080/docs`, mantenha o servidor `/` (mesma origem), use **Try it out → Execute** em cada rota e, depois do login, informe o `token` em **Authorize**. O token fica só na memória da aba.

**Por `curl`** (a partir da raiz do repositório; `jq` só extrai campos):

```bash
API=http://localhost:8080
SENHA='<senha de demonstração do database/scripts/README.md>'
```

## Passos

| # | Rota | Esperado | Observe |
|---|---|---|---|
| 1 | `POST /auth/login` | 200 | `token`, `tipo = Bearer`, `usuario.perfil = PACIENTE` |
| 2 | `GET /protocolos` | 200 | `itens[0].protocoloId = PROTOCOLO-ACADEMICO-FICTICIO-v0.1`, `simulacao = true` |
| 3 | `POST /solicitacoes` | 201 | `id`, `estado = RASCUNHO`, cabeçalho `Location` |
| 4 | `POST /solicitacoes/{id}/documentos` (encaminhamento) | 201 | `documentosDisponiveis = 1`, `versaoConjuntoDocumental = 2` |
| 5 | `POST /solicitacoes/{id}/extracoes` | 201 | `simulada = true`, `id` (leitura); `AUTORIZACAO_ASSINADA` = `NAO_LOCALIZADO` |
| 6 | `GET` e `PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` | 200 e 200 | `liberadoParaSubmissao = false`; depois de confirmar, `situacao = CONFERIDA` |
| 7 | `POST /solicitacoes/{id}/analises` | 201 | `resultadoGeral = COM_PENDENCIAS`; pendência `RC-R03` (falta a identificação) |
| 8 | `GET /solicitacoes/{id}/analises` e `GET /solicitacoes/{id}` | 200 | `vigenteId`; pedido em `COM_PENDENCIAS` |
| 9 | `POST /solicitacoes/{id}/resumo` | 201 | `resumo.simulado = true`, `resumo.origem = ADAPTADOR_MOCKADO` (ou `PADRAO` se o adaptador simulado falhar) |
| 10 | `POST /solicitacoes/{id}/documentos` (identificação) | 201 | correção: `documentosDisponiveis = 2` |
| 11 | `POST /solicitacoes/{id}/reenvio` | 200 | `etapa = CONFERENCIA`, `extracaoId` da nova leitura |
| 12 | `PUT …/extracoes/{extracaoId}/conferencia` e `POST /solicitacoes/{id}/reenvio` | 200 e 201 | `etapa = CONCLUIDA`, `analise.resultadoGeral = SEM_PENDENCIAS_IDENTIFICADAS`, `comparacao` com `RC-R03` = `RESOLVIDA` |
| 13 | `POST /solicitacoes/{id}/submissoes-simuladas` | 201 | `referencia = SIM-AAAA-NNNNNN`, `destaque = SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS`; repetir dá 200 com o mesmo comprovante |
| 14 | `GET /solicitacoes/{id}/submissoes-simuladas` | 200 | a mesma `referencia`; o pedido fica em `SUBMETIDA_SIMULACAO` (só consulta) |
| 15 | `POST /solicitacoes` + `DELETE /solicitacoes/{novoId}` com `X-Confirmar-Exclusao: EXCLUIR` | 201 e 204 | exclusão de um **rascunho descartável**; depois, `GET` dá 404 |
| 16 | `POST /auth/login` (admin) + `GET /admin/indicadores` | 200 e 200 | `dadosSinteticos = true`, só agregados; com o token da paciente, 403 |

O percurso cobre **GET, POST, PUT e DELETE** em mais de 6 rotas. Os exemplos de entrada e saída de cada rota estão no próprio contrato ([openapi.yaml](openapi.yaml)), visível em `/docs`.

## Os mesmos passos por `curl`

```bash
# 1. Login da paciente
SESSAO=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"paciente.a@exemplo.invalid\",\"senha\":\"$SENHA\"}" | jq -r .token)
AUTH="Authorization: Bearer $SESSAO"

# 2-3. Serviço e rascunho
curl -s "$API/protocolos" -H "$AUTH" | jq '.itens[0].protocoloId'
ID=$(curl -s -X POST "$API/solicitacoes" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}' | jq -r .id)

# 4. Envio do encaminhamento
curl -s -X POST "$API/solicitacoes/$ID/documentos" -H "$AUTH" \
  -F tipoDocumental=ENCAMINHAMENTO -F arquivo=@samples/encaminhamento-sintetico.pdf | jq '.documentosDisponiveis'

# 5-6. Leitura simulada e conferência (confirma todos os campos identificados)
EXT=$(curl -s -X POST "$API/solicitacoes/$ID/extracoes" -H "$AUTH" | jq -r .id)
confirmar() {
  curl -s "$API/solicitacoes/$ID/extracoes/$1/conferencia" -H "$AUTH" \
  | jq '{decisoes: [.documentos[] as $d | $d.campos[] | select(.situacao == "IDENTIFICADO")
         | {documentoId: $d.documentoId, codigo: .codigo, decisao: "CONFIRMADA"}]}' \
  | curl -s -X PUT "$API/solicitacoes/$ID/extracoes/$1/conferencia" -H "$AUTH" \
      -H 'Content-Type: application/json' --data-binary @- | jq -r .situacao
}
confirmar "$EXT"                                   # CONFERIDA

# 7-9. Análise, resultado e resumo simulado
curl -s -X POST "$API/solicitacoes/$ID/analises" -H "$AUTH" | jq '{resultadoGeral, pendencias: [.pendencias[].codigoRegra]}'
curl -s "$API/solicitacoes/$ID" -H "$AUTH" | jq -r .estado          # COM_PENDENCIAS
curl -s -X POST "$API/solicitacoes/$ID/resumo" -H "$AUTH" | jq '.resumo | {origem, simulado}'

# 10-12. Correção: inclui a identificação e verifica a versão corrigida
curl -s -X POST "$API/solicitacoes/$ID/documentos" -H "$AUTH" \
  -F tipoDocumental=IDENTIFICACAO -F arquivo=@samples/identificacao-sintetica.png | jq '.documentosDisponiveis'
EXT2=$(curl -s -X POST "$API/solicitacoes/$ID/reenvio" -H "$AUTH" | jq -r .extracaoId)   # etapa CONFERENCIA
confirmar "$EXT2"
curl -s -X POST "$API/solicitacoes/$ID/reenvio" -H "$AUTH" | jq '{etapa, resultado: .analise.resultadoGeral}'

# 13-14. Submissão simulada e comprovante
curl -s -X POST "$API/solicitacoes/$ID/submissoes-simuladas" -H "$AUTH" | jq '{referencia, destaque}'
curl -s "$API/solicitacoes/$ID/submissoes-simuladas" -H "$AUTH" | jq -r .referencia

# 15. Exclusão de um rascunho descartável
DESCARTE=$(curl -s -X POST "$API/solicitacoes" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}' | jq -r .id)
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "$API/solicitacoes/$DESCARTE" -H "$AUTH" -H 'X-Confirmar-Exclusao: EXCLUIR'   # 204

# 16. Indicadores do administrador (a paciente recebe 403)
ADMIN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"admin@exemplo.invalid\",\"senha\":\"$SENHA\"}" | jq -r .token)
curl -s "$API/admin/indicadores" -H "Authorization: Bearer $ADMIN" | jq '{dadosSinteticos, totalPedidos}'
curl -s -o /dev/null -w '%{http_code}\n' "$API/admin/indicadores" -H "$AUTH"                                               # 403
```

## Se algo der diferente

- **401 `NAO_AUTENTICADO`**: a sessão expirou (30 minutos) ou o token não foi informado; faça o login de novo.
- **503 `DEPENDENCIA_INDISPONIVEL`**: banco não configurado ou inacessível ([docs/ambiente.md](../ambiente.md)).
- **409 `CONFLITO_ESTADO`** na análise: falta a leitura da versão atual; repita o passo 5 (ou o reenvio, que lê de novo).
- **`RC-R05` pendente**: o encaminhamento sintético foi emitido em 20/10/2026, e a regra de validade conta 180 dias até a **data de referência**, que é a data (horário de Salvador) do envio mais recente entre os documentos lidos, e não a data da análise. Ela aparece em `dataReferencia` na análise.
- Os demais códigos e mensagens estão em [erros.md](erros.md).
