# Contrato de design: inventário de telas, estados e dependências da API

> **Substituído em 13/11/2026.** O inventário e o conteúdo por tela abaixo foram escritos antes das specs 008–013 e divergem do backend atual. A fonte vigente é [specs/014-fluxo-telas-app/contracts/inventario-quadros.md](../../014-fluxo-telas-app/contracts/inventario-quadros.md) (94 quadros) com [telas-operacoes.md](../../014-fluxo-telas-app/contracts/telas-operacoes.md). O épico 007 continua dono do processo de design (pen.dev, revisão, aprovação). Este arquivo fica como histórico.

Cada linha é um **quadro** (ou um conjunto de quadros) a ser desenhado. A coluna "API" é só referência para a implementação futura: **nenhuma tela chama a API**. Rotas marcadas *planejada* ainda não existem no backend; a tela segue o contrato proposto em `CONTRATO_API.md`. Convenção de nomes: `<ID> <Tela> — <Estado>` (ver `data-model.md`).

## F — Fundamentos (`00-fundamentos.pen`)

| ID | Conteúdo | Quadros |
|---|---|---|
| F1 | Variáveis: cor, tipografia, espaço, forma, toque | 1 |
| F2 | Catálogo de componentes (lista em `data-model.md`) | 1 |
| F3 | Aviso acadêmico e selo SIMULADO, com uso e proibições | 1 |
| F4 | Selos dos sete estados do pedido (texto + ícone) | 1 |

## E1 — Entrada (`01-entrada.pen`) — história 1 · origem: RC-18, RC-04 (tela)

| ID | Tela | Estados | API (referência) |
|---|---|---|---|
| E1.1 | Login | Padrão · Enviando · Credenciais inválidas · Serviço indisponível · Sem conexão | `POST /auth/login` (401 `CREDENCIAIS_INVALIDAS`, 503) |
| E1.2 | Lista de pedidos | Carregando · Vazia · Sucesso · Erro · Sem conexão | `GET /solicitacoes` |
| E1.3 | Detalhe do pedido | Carregando · um por estado do pedido (`RASCUNHO`, `PROCESSANDO`, `REQUER_CONFERENCIA`, `COM_PENDENCIAS`, `SEM_PENDENCIAS_IDENTIFICADAS`, `FALHA_ANALISE`, `SUBMETIDA_SIMULACAO`) · Não encontrado | `GET /solicitacoes/{id}` (404) |
| E1.4 | Sessão expirada | Padrão (volta ao login) | 401 `NAO_AUTENTICADO` |
| E1.5 | Sem permissão | Padrão | 403 `ACESSO_NEGADO` |

Quadros: 5 + 5 + 9 + 1 + 1 = **21**.

## E2 — Envio (`02-envio.pen`) — história 2 · origem: RC-07, RC-08, RC-09

| ID | Tela | Estados | API (referência) |
|---|---|---|---|
| E2.1 | Rascunho e serviço (indicação de protocolo acadêmico) | Padrão · Salvando · Erro · Campos preservados após falha | `GET /protocolos` (*planejada*), `POST/PUT /solicitacoes` |
| E2.2 | Documentos do pedido (nome, tipo, tamanho, estado, abrir; limites visíveis) | Vazia · Com arquivos (aguardando, enviando, enviado, falhou) · Enviando · Interrompido (tentar de novo) | `POST/GET /solicitacoes/{id}/documentos[/{docId}]` (*planejada*) |
| E2.3 | Recusa de arquivo | Tipo não suportado · Extensão incompatível · Maior que 5 MB · PDF com mais de 5 páginas · PDF protegido · Arquivo ilegível · Mais de 3 arquivos | 415 `TIPO_NAO_SUPORTADO`, 415 `EXTENSAO_INCOMPATIVEL`, 413 `ARQUIVO_MUITO_GRANDE`, 400 `REQUISICAO_INVALIDA` (`docs/api/protecoes.md`) |
| E2.4 | Confirmar substituição de documento | Confirmação | `PUT/POST` de documento (*planejada*) |
| E2.5 | Confirmar remoção de documento | Confirmação | `DELETE .../documentos/{docId}` (*planejada*) |
| E2.6 | Confirmar exclusão de rascunho | Confirmação | `DELETE /solicitacoes/{id}` (*planejada*) |

Quadros: 4 + 4 + 7 + 3 = **18**.

## E3 — Resultado (`03-resultado.pen`) — história 3 · origem: RC-11, RC-15, RC-16, RC-17

| ID | Tela | Estados | API (referência) |
|---|---|---|---|
| E3.1 | Leitura simulada dos arquivos | Processando (selo SIMULADO) | `POST /solicitacoes/{id}/extracoes` (*planejada*) |
| E3.2 | Conferência da transcrição (valor, origem, status; confirmar ou marcar divergência) | Padrão · Divergência marcada · Campo ausente (orienta unidade emissora) | `PUT .../extracoes/{extracaoId}/conferencia` (*planejada*) |
| E3.3 | Resultado da análise (resultado geral, resumo SIMULADO, pendências) | Processando · Com pendências · Requer conferência · Sem pendências identificadas · Falha na análise (tentar de novo) | `POST/GET /solicitacoes/{id}/analises` (*planejada*) |
| E3.4 | Detalhe da pendência (critério, versão do protocolo, trecho de origem, responsável) | Reenviar (foto cortada ou arquivo ausente) · Procurar a unidade emissora (informação clínica ausente) | `GET .../analises` (*planejada*) |
| E3.5 | Reenvio e comparação de pendências | Antes do novo resultado (nada marcado como resolvido) · Comparação após o novo resultado | `POST .../documentos`, `GET .../analises` (*planejada*) |
| E3.6 | Comprovante da submissão simulada (destaque SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS) | Padrão · Operação repetida (mesmo comprovante) | `POST /solicitacoes/{id}/submissoes-simuladas` (*planejada*) |

Quadros: 1 + 3 + 5 + 2 + 2 + 2 = **15**.

## E4 — Sensores, administrador e acessibilidade (`04-sensores-admin-acessibilidade.pen`) — história 4 · origem: RC-22, RC-20, RC-19

| ID | Tela | Estados | API (referência) |
|---|---|---|---|
| E4.1 | Estação de captura (lux e cm) | Normal · Luminosidade fora da faixa · Distância fora da faixa · Recuperada · Leitura desatualizada · Sensores indisponíveis (não bloqueia envio) | `GET /solicitacoes/{id}/leituras` (*planejada*) |
| E4.2 | Visão do administrador (protocolo ativo, regras, totais; sem dado individual) | Padrão · Carregando · Sem permissão (paciente tentando) | `GET /admin/indicadores` (*planejada*; 403 para paciente) |
| E4.3 | Conexão ruim | Tempo esgotado (tentar novamente, campos preservados) · Sem conexão | falha de rede; sem chamada |
| E4.4 | Guia de acessibilidade aplicado | Anotações (contraste, alvos de toque, leitura) · Fonte ampliada em E1.2, E3.3 e E2.2 (3 quadros) | — |

Quadros: 6 + 3 + 2 + 4 = **15**.

## Matriz tela × estado (critério de revisão de FR-002 e SC-001)

Legenda: **P** = quadro próprio (já listado acima) · **C** = coberto por quadro ou componente compartilhado, com **nota na tela** apontando para ele · **—** = não se aplica.

Compartilhados: **Carregando** → componente "esqueleto de carregamento" (F2); **Erro genérico** → componente "faixa de erro" (F2) com "tentar novamente"; **Sem conexão / tempo esgotado** → quadro E4.3 e componente "faixa de sem conexão"; **401** → E1.4; **403** → E1.5; **404** → E1.3 "Não encontrado".

| Tela | Carregando | Vazia | Erro | Sem conexão | 401 | 403 | 404 |
|---|---|---|---|---|---|---|---|
| E1.1 Login | P (Enviando) | — | P (credenciais inválidas, serviço indisponível) | P | — (rota pública) | — | — |
| E1.2 Lista | P | P | P | P | C | C | — |
| E1.3 Detalhe | P | — | C | C | C | C | P |
| E2.1 Rascunho | P (Salvando) | — | P | C | C | C | C |
| E2.2 Documentos | P (Enviando) | P | P (Falhou, Interrompido, recusas E2.3) | C | C | C | C |
| E2.3 a E2.6 (recusas e confirmações) | — | — | — | — | C | C | C |
| E3.1 Leitura simulada | P (Processando) | — | C | C | C | C | C |
| E3.2 Conferência | C | — | C | C | C | C | C |
| E3.3 Resultado | P (Processando) | — | P (Falha na análise) | C | C | C | C |
| E3.4 Detalhe da pendência | C | — | C | C | C | C | C |
| E3.5 Reenvio e comparação | C | — | C | C | C | C | C |
| E3.6 Comprovante | C | — | C | C | C | C | C |
| E4.1 Estação de captura | C | P (Sensores indisponíveis) | P (Sensores indisponíveis) | C | C | C | C |
| E4.2 Administrador | P | — | C | C | C | P | — |
| E4.3 Conexão ruim | — | — | P | P | — | — | — |

## Casos de borda e onde são tratados

| Caso de borda | Tratamento |
|---|---|
| Tentar alterar documento com o pedido em `PROCESSANDO` | Nota e texto no quadro E1.3 `PROCESSANDO` e na tela E2.2: uma nova revisão será iniciada e o resultado anterior deixará de valer |
| Fonte ampliada e aparelho pequeno | Quadro E4.4 mostra três telas representativas (E1.2, E3.3 e E2.2) com fonte ampliada, sem cortar conteúdo essencial |
| Nome de arquivo longo, acentuado ou estranho | Linha de documento do catálogo (F2) com truncamento e nome completo ao abrir |
| Texto com `<`, `>`, `&` | Quadros E3.3 (resumo) e E3.4 (trecho de origem) mostram o símbolo como texto |
| Muitos pedidos | E1.2 Sucesso com rolagem e ordenação pela última atualização |
| Conta que perde a permissão | E1.4 (401) e E1.5 (403) |
| Primeiro uso sem pedidos | E1.2 Vazia |

## Totais

F 4 + E1 21 + E2 18 + E3 15 + E4 15 = **73 quadros**.

## Regras de conteúdo (valem para todos os quadros)

1. Aviso acadêmico visível em toda tela.
2. Selo SIMULADO em extração, resumo e comprovante.
3. Estado sempre por texto + ícone, nunca só cor.
4. Linguagem proibida: **afirmar** diagnóstico, urgência, autorização de atendimento, "aprovado pelo SUS", vaga garantida, promessa de prazo ou de redução de fila. **Negações e avisos** ("isto não é aprovação do SUS", "não entra em fila real") são obrigatórios e permitidos; a busca de termos lista as ocorrências e cada uma é conferida à mão.
9. Cada tela traz uma nota com a rota da API e o contrato de que depende, e, nas células "C" da matriz, a indicação do quadro ou componente que cobre o estado.
5. Informação clínica ausente → "procure a unidade emissora do documento"; nunca oferecer preencher o dado como se constasse no documento.
6. Texto vindo de documento ou do resumo é desenhado como texto puro.
7. 401 → tela de sessão expirada e login; 403 → sem permissão; 404 → item não encontrado.
8. Nenhuma credencial, token, chave ou dado de pessoa real desenhado.
