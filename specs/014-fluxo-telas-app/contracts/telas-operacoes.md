# Contrato de design: tela × operação

Para cada ação desenhada, a operação do backend que ela usa e o que a tela faz com cada resposta. É o critério de SC-001 (nenhuma ação sem operação) e de FR-001. **Situação**: ✅ implementada em `main` · † contratada, ainda não implementada (anotar o quadro como "contrato provisório").

Respostas comuns a toda rota privada (FR-010): **401** → E1.4 Sessão expirada · **403** → E1.5 Sem permissão · **404** de pedido → E1.3 Não encontrado · **503** `DEPENDENCIA_INDISPONIVEL` → `Retorno/Erro` "O serviço está indisponível agora. Tente de novo." · falha de rede ou tempo esgotado → E4.3. Não repetidas abaixo.

## Entrada e pedidos (RC-36)

| Tela · ação | Operação | Situação | Sucesso | Respostas tratadas |
|---|---|---|---|---|
| E1.1 Entrar | `POST /auth/login` | ✅ RC-04 | perfil `PACIENTE` → E1.2; `ADMINISTRADOR` → E4.2 | 401 `CREDENCIAIS_INVALIDAS` → "E-mail ou senha incorretos." (e-mail mantido, senha limpa) · 400 → campo indicado · 503 → serviço indisponível |
| E1.2 Abrir | `GET /auth/sessao`, `GET /solicitacoes` | ✅ RC-04, RC-07 | lista (até 50) | vazia → `Vazio/Pedidos` |
| E1.2 Sair | `POST /auth/logout` | ✅ RC-04 | E1.1 | qualquer falha → E1.1 (sessão local descartada) |
| E1.3 Abrir | `GET /solicitacoes/{id}`; `GET …/documentos`; `GET …/extracoes`; `GET …/extracoes/{extracaoId}/conferencia`; `GET …/analises` † | ✅ / † | dados + ação de FR-012 | `GET …/extracoes` 404 = sem leitura (não é erro) |
| E1.3 Atualizar (em verificação) | `GET /solicitacoes/{id}` | ✅ | estado novo | — |
| E1.3 Tentar de novo (falha) | `POST …/analises` † | † RC-13 | E1.3 "Em verificação" ou resultado | mesmas de "Verificar critérios" |

## Envio (RC-37)

| Tela · ação | Operação | Situação | Sucesso | Respostas tratadas |
|---|---|---|---|---|
| E2.1 Abrir | `GET /protocolos` | ✅ RC-07 | lista de serviços | `itens` vazio → "Nenhum serviço disponível agora." |
| E2.1 Criar rascunho | `POST /solicitacoes` | ✅ RC-07 | 201 → E2.2 do pedido novo | 422 → "Escolha um dos serviços da lista." · botão bloqueado durante a chamada (o backend não deduplica toque duplo) |
| E2.1 Salvar serviço (troca) | `PUT /solicitacoes/{id}` | ✅ RC-07 | E1.3 | 409 → "Somente rascunhos podem trocar de serviço." · 422 como acima |
| E2.2 Abrir | `GET …/documentos` | ✅ RC-08 | linhas | vazia → estado Vazia |
| E2.7 → Enviar arquivo | `POST …/documentos` (multipart `arquivo` + `tipoDocumental`; `X-Chave-Envio` por tentativa) | ✅ RC-08 | 201/200 → linha "Enviado" | 415 `TIPO_NAO_SUPORTADO` · 415 `EXTENSAO_INCOMPATIVEL` · 413 `ARQUIVO_MUITO_GRANDE` · 400 (`arquivo`: ilegível, PDF com senha, > 5 páginas) · 422 limite de 3 · 409 `CONFLITO_ESTADO` "Este pedido não pode mais receber documentos." → E2.3 (uma recusa por quadro) · 409 `CHAVE_ENVIO_REUTILIZADA` (repetição com outro conteúdo) → E2.2 Não enviado com "Escolha o arquivo outra vez." |
| E2.2 Abrir arquivo | `GET …/documentos/{docId}` | ✅ RC-08 | visualizador do sistema | 404 → "Este documento não está mais no pedido." |
| E2.4 Substituir | `PUT …/documentos/{docId}` (multipart, `X-Chave-Envio`) | ✅ RC-09 | 201/200 → linha nova "Enviado"; se pedido verificado → aviso de resultado não vigente | mesmas recusas do envio (o antigo fica intacto) · 409 submetido |
| E2.5 Remover | `DELETE …/documentos/{docId}` | ✅ RC-09 | 200 → lista sem a linha | 404 → lista recarregada · 409 submetido |
| E2.6 Excluir rascunho | `DELETE /solicitacoes/{id}` + `X-Confirmar-Exclusao: EXCLUIR` | ✅ RC-09 | 204 → E1.2 | 409 → "Somente rascunhos podem ser excluídos." |

## Leitura, conferência, verificação e conclusão (RC-38)

| Tela · ação | Operação | Situação | Sucesso | Respostas tratadas |
|---|---|---|---|---|
| E3.1 Ler documentos | `POST …/extracoes` | ✅ RC-10 | 201/200 → E3.2 documento 1 | 422 `SEM_DOCUMENTOS` → "Anexe ao menos um documento antes de ler." · 503 `LEITURA_INDISPONIVEL` → "A leitura simulada não está disponível agora. Tente de novo." · 409 documentos mudaram → "Os documentos mudaram; tente de novo." · 409 submetido |
| E3.2 Abrir | `GET …/extracoes/{extracaoId}/conferencia` | ✅ RC-11 | campos do documento i | `situacao = DESATUALIZADA` → só consulta |
| E3.2 Salvar e continuar | `PUT …/conferencia` (decisões do documento i) | ✅ RC-11 | 200 → documento i+1 ou E1.3 | 409 desatualizada → "Os documentos mudaram; leia os documentos de novo." · 422 `DECISAO_NAO_APLICAVEL` → "Não foi possível salvar esta decisão." (não deve ocorrer: a tela só oferece as aceitas) · 400 → "Não foi possível salvar. Tente de novo." · nada é salvo em erro |
| E3.8 → Verificar critérios | `POST …/analises` | † RC-13 | 202 → E1.3 "Em verificação"; 200 → E3.3 | 409 → mensagem do backend · falha → `FALHA_ANALISE` |
| E3.3 Abrir | `GET …/analises` | † RC-13 | resultado vigente (+ anterior para comparação) | `vigente = false` → aviso de resultado não vigente |
| E3.3 Tentar resumo de novo | `POST …/analises/{analiseId}/resumo` | † RC-14 | resumo | falha → texto padrão identificado |
| E3.4 Abrir documento | `GET …/documentos/{docId}` | ✅ RC-08 | visualizador | 404 → "Este documento não está mais no pedido." |
| E3.4 Já tenho uma nova versão / Substituir arquivo | E2.7 + `PUT …/documentos/{docId}` | ✅ RC-09 | aviso de resultado não vigente | como E2.4 |
| E3.4 Conferir leitura | navegação a E3.2 | ✅ | — | — |
| E3.5 Ler documentos de novo | `POST …/extracoes` | ✅ RC-10 | E3.2 (nova leitura, sem decisões herdadas) | como E3.1 |
| E3.7 Concluir simulação | `POST …/submissoes-simuladas` | † RC-17 | 201 → E3.6; repetição → o mesmo comprovante | 409 (sem resultado vigente sem pendências) → mensagem do backend |
| E3.6 Ver comprovante | mesma submissão (repetição idempotente) ou consulta a definir na RC-17 | † RC-17 | E3.6 | — |

## Sensores, administrador e transversais (RC-39)

| Tela · ação | Operação | Situação | Sucesso | Respostas tratadas |
|---|---|---|---|---|
| E4.1 Abrir e atualizar | `GET /solicitacoes/{id}/leituras` (repetida enquanto aberta) | † RC-21/22 | valores, classificação, instante | sem leitura ou erro → "Sensores indisponíveis agora." (não bloqueia) |
| E4.1 Fotografar | câmera do aparelho; depois `POST …/documentos` | ✅ RC-08 | linha "Enviado" | recusas de E2.3 |
| E4.2 Abrir | `GET /admin/indicadores` | † RC-20 | protocolo, regras, totais | 403 (paciente) → E1.5 |
| E4.2 Sair | `POST /auth/logout` | ✅ RC-04 | E1.1 | — |

## Navegação e ações locais (sem operação própria)

Ações desenhadas que só mudam de tela ou ajustam a escolha antes de salvar. Registradas na auditoria T057 (14/11/2026) para que toda ação dos quadros tenha correspondência (SC-001).

| Tela · ação | Efeito | Operação usada depois |
|---|---|---|
| E1.2 Abrir pedido (linha) · E1.3 linhas de navegação (Documentos, Leitura e conferência, Resultado) | abre E1.3, E2.2, E3.2 ou E3.3 | as de "Abrir" da tela de destino |
| E1.2 Novo pedido | abre E2.1 | `POST /solicitacoes` em E2.1 |
| E1.3 / E2.1 / E3.6 Voltar para meus pedidos · E1.5 Voltar · E2.3 Voltar ao pedido | volta à tela anterior | — |
| E1.3 Mais opções (rascunho) → Trocar serviço · Excluir rascunho | abre E2.1 (troca) ou E2.6 | `PUT /solicitacoes/{id}` · `DELETE /solicitacoes/{id}` |
| E1.4 Entrar | abre E1.1 | `POST /auth/login` |
| E2.2 Anexar documento · E2.3 Escolher outro arquivo · E3.1 Anexar documento | abre E2.7 (tipo e origem) | `POST …/documentos` |
| E2.2 Mais opções do arquivo → Substituir · Remover | abre E2.4 ou E2.5 | `PUT` / `DELETE …/documentos/{docId}` |
| E2.7 Fotografar · E4.1 Fotografar / Fotografar mesmo assim · E4.1 Escolher arquivo | câmera ou seletor do aparelho | `POST …/documentos` (ou `PUT` na substituição) |
| E2.7 / Folhas Cancelar · Diálogos Cancelar / Voltar | fecha a folha ou o diálogo sem chamada | — |
| E3.2 Mudar decisão | reabre as decisões do campo antes de salvar | `PUT …/conferencia` em "Salvar e continuar" |
| E3.2 Substituir arquivo (documento não lido ou campo ilegível) | abre E2.7 em modo substituição | `PUT …/documentos/{docId}`, como E2.4 |
| E3.3 Conferir leitura · E3.4 Conferir leitura | abre E3.2 | `PUT …/conferencia` |
| E3.3 Concluir simulação · E1.3 Concluir simulação | abre E3.7 | `POST …/submissoes-simuladas` † |
| E4.3 Tentar novamente | repete a operação que falhou, com os dados mantidos | a mesma operação. No envio de arquivo, a mesma `X-Chave-Envio` evita duplicar. Em `POST /solicitacoes`, repetir depois de um tempo esgotado pode criar um segundo rascunho, porque o backend não deduplica; a tela não promete o contrário |

## Operações que as telas **não** usam

`POST …/documentos` fora de rascunho (409), `DELETE /solicitacoes/{id}` fora de rascunho (409), decisão em campo ilegível ou confirmação de campo não localizado (422), qualquer valor digitado na conferência (400), `POST /sessoes-captura/{id}/leituras` (credencial do simulador, não da paciente), rotas de administrador para paciente e vice-versa.

## Limitação registrada para a RC-16

Depois da primeira verificação não há operação para acrescentar documento. Pendência de documento ausente (R01, R03, R06) só tem como caminho substituir um documento enviado. A tela explica; a decisão de mudar o contrato é do dono da RC-16. Evidência: a carga sintética (`database/scripts/02_carga_sintetica.sql`, Pedido 1) registra uma identificação enviada na revisão 2, depois de uma análise — caminho que a API atual recusa.
