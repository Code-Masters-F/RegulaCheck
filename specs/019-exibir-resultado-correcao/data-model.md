# Data model: apresentação do resultado (RC-15)

Modelo de apresentação derivado dos contratos RC-13/RC-14. Nenhuma tabela, migração ou entidade persistida nova é proposta.

| Conceito | Dados necessários | Fonte e relação | Regras de apresentação |
|---|---|---|---|
| Pedido consultado | identificador, estado, revisão atual | retrato de `GET /solicitacoes/{id}/analises` | `vigenteId` identifica a análise atual; ausência não autoriza inferir resultado. |
| Análise selecionada | id, revisão, `vigente`, momento, resultado geral, protocolo (identificador e versão), aviso | item de `analises[]` | Resumo, pendências e evidências vêm deste mesmo item. Se `vigente=false`, rotular “Resultado anterior”. |
| Resumo | texto, `origem`, `simulado`, itens por regra | `resumo` da análise; pode estar ausente | Sempre rotular SIMULADO. `PADRAO` ou ausência mantém pendências visíveis e oferece nova tentativa quando a análise ainda é vigente. |
| Pendência | código e descrição da regra, tipo, explicação, responsável, motivo opcional | `pendencias[]` da análise | Exibir um detalhe por pendência; não converter texto livre em novas regras ou ações clínicas. |
| Evidência | documentoId, campo, situação, origem opcional com página e trecho | `evidencias[]` da pendência | Nome do documento vem de `documentos[]` da mesma análise. Omitir página/trecho não disponíveis; texto é literal. |
| Ação apresentada | orientação textual, responsável registrado pela regra e destino opcional de navegação | decisão baseada na pendência, evidência e operações permitidas | Preservar `responsavel` da API; em RC-R02, orientar conferência e condicionar a solicitação à unidade a erro no original, sem inferir qual documento está errado. Sem ação de editar conteúdo clínico; sem botão de anexar depois da primeira análise quando a API o recusar. Explicar a limitação e orientar a obter arquivo ausente para reenvio quando disponível. |

## Estados transitórios da apresentação

| Estado | Condição | Conteúdo autorizado |
|---|---|---|
| Carregando | consulta em andamento | progresso, sem resultado presumido |
| Sem análise | lista vazia, sem `vigenteId` | explicar que ainda não há verificação concluída |
| Em verificação | `execucao.situacao=EM_ANDAMENTO` | andamento; sem resultado novo |
| Resultado vigente | `vigenteId` encontra uma análise concluída | estado, resumo disponível ou padrão, pendências e detalhes daquela análise |
| Resultado anterior | análise consultada sem vigência | revisão e aviso “Resultado anterior”; não apresentar como conclusão atual |
| Falha recuperável | rede ou serviço indisponível | explicação e nova consulta; preservar dados determinísticos já confirmados sem dizer que são atuais se a vigência ficou desconhecida |

**Invariantes**: uma tela não combina dados de análises distintas; `NAO_VERIFICAVEL` não vira “ausente”; sem `origem` não há trecho; `simulado` e limites acadêmicos ficam explícitos; saída do resumo não substitui resultados por regra.
