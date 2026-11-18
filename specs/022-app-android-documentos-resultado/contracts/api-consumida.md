# Contrato consumido — RC-19

Fonte: `docs/api/openapi.yaml` e `docs/api/erros.md` (versão da `main`, com a RC-15), [contrato de apresentação da spec 019](https://github.com/Code-Masters-F/RegulaCheck/blob/main/specs/019-exibir-resultado-correcao/contracts/apresentacao-resultado.md). O app não cria rota nem altera o backend. Regras comuns (401 → login com sessão expirada; cabeçalhos; mapeamento HTTP → `ErroApi`): [contrato da RC-18](../017-app-android-login-pedidos/contracts/api-consumida.md).

| # | Tela · ação | Rota | Sucesso | Tratamento no app |
|---|---|---|---|---|
| 1 | E2.1 Abrir | `GET /protocolos` | primeiro de `itens` | vazio → "Nenhum serviço disponível agora." sem criar · rede/5xx → retorno + "Tentar de novo" · 403 sem permissão |
| 2 | E2.1 Criar rascunho | `POST /solicitacoes` `{protocoloId}` | 201 → E2.2 do pedido | botão "Criando…" · 422 → "Escolha um dos serviços da lista." · tempo esgotado → E4.3 + conferir "Meus pedidos" · rede/5xx → "Não foi possível criar o rascunho." + "Tentar de novo" |
| 3 | E2.2 Abrir | `GET /solicitacoes/{id}` + `GET …/documentos` | dados + lista | 404 → não encontrado · 403 → sem permissão · rede/5xx → retorno + "Tentar de novo" |
| 4 | E2.2 Enviar | `POST …/documentos` multipart (`arquivo`, `tipoDocumental`), `X-Chave-Envio` | 201 novo / 200 repetição → entra na lista | 400/413/415/422 → faixa com `mensagem` da API, "Nada foi salvo." · 409 `CHAVE_ENVIO_REUTILIZADA` → escolher de novo · 409 `CONFLITO_ESTADO` → pedido fechado · rede/tempo/5xx → "Não enviado" + "Tentar de novo" (mesma chave) · arquivo inacessível → escolher de novo |
| 5 | E3.1 Ler | `POST …/extracoes` (70 s) | 201/200 → situação por documento | 422 `SEM_DOCUMENTOS` → "Anexe ao menos um documento antes de ler." · 409 → mensagem da API + "Ler documentos" · 503 → "A leitura simulada não está disponível agora." + "Tentar de novo" · rede/tempo → E4.3 + "Tentar novamente" |
| 6 | E3.8 Verificar | `POST …/analises` (70 s) | 201/200 → (resumo #8 se ausente) → E3.3 | 409 `ANALISE_EM_ANDAMENTO` → espera de `Retry-After` + "Consultar resultado" · 409 sem leitura/mudou → mensagem da API + "Ler documentos" · 503 → "A verificação não pôde ser feita." + "Tentar de novo" · rede/tempo → #7 antes de qualquer nova tentativa |
| 7 | E3.3 Abrir / após falha da análise | `GET …/analises` | histórico | seleção da vigente (`vigenteId` + `vigente`) · rede/503 → "Não foi possível consultar o resultado agora." + "Tentar novamente" · 403/404 como a RC-18 |
| 8 | E3.3 Resumo | `POST …/resumo` | 201/200 → análise com `resumo` | 409 → reconsultar #7 · rede/5xx → resumo continua indisponível + "Tentar de novo"; resultado e pendências intactos |

**Envio (4)**: `Content-Type: multipart/form-data`; parte `arquivo` com `filename` = nome de exibição do arquivo escolhido e `Content-Type` do provedor; parte `tipoDocumental` com `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO`. A chave nasce na escolha do arquivo e acompanha todas as tentativas daquele arquivo.

**Testes MockWebServer (#42)**: envio 201 e 200 com a mesma chave; 413 e 415 → `ArquivoRecusado` com a mensagem; 422 `SEM_DOCUMENTOS`; 409 sem leitura; 409 `ANALISE_EM_ANDAMENTO` com `Retry-After`; 503 na leitura e na análise.
