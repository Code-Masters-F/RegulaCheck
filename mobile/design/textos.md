# Textos da interface

> Todos os textos exibidos nas telas, por quadro e estado. **O desenho deve usar exatamente estes textos**; alterou o desenho, altere aqui. Dados fictícios: [dados-ficticios.md](dados-ficticios.md). Regras de linguagem: [checklist-revisao.md](checklist-revisao.md).
> Convenção: `{...}` = valor que vem dos dados fictícios. Textos em português do Brasil, frases curtas, sempre dizendo o próximo passo.

## F3 — Avisos fixos (componentes)

| Componente | Texto |
|---|---|
| Aviso acadêmico (rodapé de toda tela) | "Aplicativo acadêmico com dados fictícios. O protocolo é de teste e não é exigência oficial. Não substitui atendimento nem orientação de saúde." |
| Aviso acadêmico curto (faixa em telas estreitas) | "Protótipo acadêmico · dados fictícios" |
| Selo SIMULADO | "SIMULADO" |
| Legenda do selo (toque longo ou rodapé do bloco) | "Conteúdo de demonstração. Não é leitura nem inteligência artificial reais." |
| Destaque do comprovante | "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" |

---

## E1 — Entrada

### E1.1 Login
| Estado | Textos |
|---|---|
| Padrão | Título: "Entrar". Campos: "E-mail", "Senha". Botão: "Entrar". Aviso acadêmico. |
| Enviando | Botão desabilitado: "Entrando…" com indicador de progresso. Campos preservados. |
| Credenciais inválidas | "E-mail ou senha incorretos. Confira os dados e tente de novo." E-mail mantido; senha limpa; botão "Entrar" habilitado. |
| Serviço indisponível | "O serviço está indisponível agora. Tente de novo em alguns instantes." Botão "Tentar de novo". E-mail preservado. |
| Sem conexão | "Sem conexão com a internet. Verifique sua rede e tente de novo." Botão "Tentar de novo". E-mail preservado. |

### E1.2 Lista de pedidos
| Estado | Textos |
|---|---|
| Carregando | Título: "Meus pedidos". Esqueleto de carregamento. Texto de apoio: "Carregando seus pedidos…" |
| Vazia | "Você ainda não tem pedidos." / "Toque em “Novo pedido” para começar a preparar seus documentos." Botão: "Novo pedido". |
| Sucesso | Título: "Meus pedidos". Cada cartão: "{código do pedido}", "{serviço}", selo de estado (texto + ícone), "Atualizado em {data e hora}". Botão: "Novo pedido". Ordem: do mais recente para o mais antigo. |
| Erro | "Não foi possível carregar seus pedidos." / "Tente de novo. Se o problema continuar, volte mais tarde." Botão: "Tentar de novo". |
| Sem conexão | "Sem conexão com a internet." / "Seus pedidos aparecerão quando a conexão voltar." Botão: "Tentar de novo". |

### F4 / E1.3 — Selos de estado e próxima ação (detalhe do pedido)
| Estado do pedido | Texto do selo | Próxima ação (texto no detalhe) |
|---|---|---|
| `RASCUNHO` | "Rascunho" | "Anexe os documentos e envie para conferência." Botões: "Anexar documentos", "Excluir rascunho". |
| `PROCESSANDO` | "Em conferência" | "A conferência ainda não terminou. Aguarde: isso não significa que o pedido foi concluído." Se tentar alterar documentos: "Se você alterar um documento agora, uma nova revisão será iniciada e o resultado anterior deixará de valer." |
| `REQUER_CONFERENCIA` | "Precisa da sua conferência" | "Confira a leitura dos seus documentos antes de continuar." Botão: "Conferir leitura". |
| `COM_PENDENCIAS` | "Com pendências" | "Veja o que precisa ser corrigido e providencie." Botão: "Ver pendências". |
| `SEM_PENDENCIAS_IDENTIFICADAS` | "Sem pendências identificadas" | "Não encontramos pendências nos critérios de teste. Isso não é aprovação do SUS nem garantia de vaga." Botão: "Concluir simulação". |
| `FALHA_ANALISE` | "Falha na conferência" | "A conferência não pôde ser feita. Você pode tentar de novo." Botão: "Tentar de novo". |
| `SUBMETIDA_SIMULACAO` | "Simulação concluída" | "A simulação foi concluída. Nada foi enviado ao SUS." Botão: "Ver comprovante". |

Campos do detalhe (todos os estados): "Pedido {código}", "Serviço: {serviço}", "Localidade: Município Exemplo (fictício)", "Protocolo: PROTOCOLO-ACADEMICO-FICTICIO v0.1 (de teste)", "Revisão {número}", "Atualizado em {data e hora}".

### E1.3 Detalhe — Carregando e Não encontrado
| Estado | Textos |
|---|---|
| Carregando | Esqueleto; "Carregando o pedido…" |
| Não encontrado | "Pedido não encontrado." / "Ele pode ter sido excluído ou não estar disponível para você." Botão: "Voltar para meus pedidos". |

### E1.4 Sessão expirada
"Sua sessão terminou." / "Por segurança, entre de novo para continuar." Botão: "Entrar". (Leva ao login; não mostra dados do pedido.)

### E1.5 Sem permissão
"Você não tem permissão para ver esta tela." Botão: "Voltar".

---

## E2 — Envio

### E2.1 Rascunho e serviço
| Estado | Textos |
|---|---|
| Padrão | Título: "Novo pedido". "Localidade" (seletor): "Município Exemplo (fictício)". "Serviço" (seletor): "Consulta ambulatorial em cardiologia". Faixa informativa: "Protocolo acadêmico fictício (PROTOCOLO-ACADEMICO-FICTICIO v0.1). Não é exigência oficial." Botões: "Salvar rascunho", "Cancelar". |
| Salvando | Botão: "Salvando…" desabilitado; campos preservados. |
| Erro | "Não foi possível salvar o rascunho. Seus dados continuam na tela." Botão: "Tentar de novo". |
| Campos preservados após falha | Mesmo formulário preenchido + faixa: "Não conseguimos salvar. Nada foi perdido: confira os dados e tente de novo." |

### E2.2 Documentos do pedido
| Estado | Textos |
|---|---|
| Vazia | Título: "Documentos". "Nenhum documento anexado." / "Anexe o encaminhamento e o documento de identificação ou cartão do SUS (exemplos de teste)." Botão: "Anexar arquivo". Limites: "Até 3 arquivos · 5 MB cada · PDF (até 5 páginas), JPEG ou PNG." |
| Com arquivos | Linha: "{nome do arquivo}", "{tipo} · {tamanho}", estado: "Aguardando", "Enviando…", "Enviado" ou "Falhou" (texto + ícone), ação "Abrir". Ações: "Substituir", "Remover". Botão "Anexar arquivo" (desabilitado com "Limite de 3 arquivos atingido" quando houver 3). Nome longo: truncado no meio, nome completo ao abrir. |
| Enviando | "Enviando “{nome do arquivo}”…" com progresso. Texto: "Mantenha o aplicativo aberto até o envio terminar." |
| Interrompido | "O envio de “{nome do arquivo}” foi interrompido." / "O arquivo ainda não foi enviado. Toque em “Tentar de novo”." Se for preciso escolher o arquivo de novo: "Escolha o arquivo outra vez." Botões: "Tentar de novo", "Remover". |
| Pedido em conferência | Faixa: "Se você alterar um documento agora, uma nova revisão será iniciada e o resultado anterior deixará de valer." |

### E2.3 Recusa de arquivo (mensagens do contrato `docs/api/protecoes.md`)
| Motivo | Texto exibido | Ação |
|---|---|---|
| Tipo não suportado | "Envie um arquivo PDF, JPEG ou PNG." | "Escolher outro arquivo" |
| Extensão incompatível | "A extensão do arquivo não corresponde ao seu conteúdo. Escolha o arquivo original, sem renomear." | "Escolher outro arquivo" |
| Maior que 5 MB | "O arquivo excede 5 MB. Reduza o tamanho ou tire uma nova foto." | "Escolher outro arquivo" |
| PDF com mais de 5 páginas | "O PDF pode ter no máximo 5 páginas. Envie só as páginas necessárias." | "Escolher outro arquivo" |
| PDF protegido | "PDF protegido por senha. Envie uma versão sem proteção ou um novo arquivo." | "Escolher outro arquivo" |
| Arquivo ilegível | "Não foi possível ler o arquivo. Envie um novo arquivo." | "Escolher outro arquivo" |
| Mais de 3 arquivos | "No máximo 3 arquivos por pedido. Remova um para anexar outro." | "Ver meus arquivos" |

Todas as recusas dizem: "Nada foi salvo." e que é preciso selecionar o arquivo de novo.

### E2.4 a E2.6 Confirmações
| Quadro | Textos |
|---|---|
| E2.4 Substituir | Título: "Substituir documento?" / "“{nome do arquivo}” será trocado pelo novo arquivo. A conferência anterior deixará de valer e será preciso conferir de novo." Botões: "Cancelar" (destaque), "Substituir". |
| E2.5 Remover | Título: "Remover documento?" / "“{nome do arquivo}” será removido do pedido. A conferência anterior deixará de valer." Botões: "Cancelar" (destaque), "Remover". |
| E2.6 Excluir rascunho | Título: "Excluir este rascunho?" / "O rascunho {código} e os {número} documentos anexados serão apagados. Isso não pode ser desfeito." Botões: "Cancelar" (destaque), "Excluir rascunho". |

---

## E3 — Resultado

### E3.1 Leitura simulada
"Lendo seus documentos…" Selo SIMULADO. Texto: "Esta é uma leitura simulada, de demonstração. Ela não lê o conteúdo real do arquivo."

### E3.2 Conferência da transcrição
| Estado | Textos |
|---|---|
| Padrão | Título: "Confira a leitura". Selo SIMULADO. Cada campo: "{campo}", "{valor}", "Origem: {arquivo}, página {n}", status por texto + ícone: "Identificado", "Não localizado", "Ilegível". Para os não verificados: "Não foi possível confirmar este campo." Ações por campo: "Está correto", "Marcar divergência". Botão geral: "Concluir conferência". |
| Divergência marcada | Campo com "Divergência marcada" e opção "Desfazer". Texto: "Anotamos que a leitura difere do documento. O documento original não é alterado." |
| Campo ausente | Campo "Motivo do encaminhamento": "Não localizado no documento." / "Se a informação não está no documento, procure a unidade emissora para completá-lo. Você não pode preencher este dado aqui." Sem campo de digitação. |

### E3.3 Resultado da análise
| Estado | Textos |
|---|---|
| Processando | "Conferindo seus documentos…" / "Isso não significa que o pedido foi concluído." |
| Com pendências | Título do resultado: "Com pendências". Resumo (selo SIMULADO): texto do resumo simulado de `dados-ficticios.md`. Lista: "Pendência: {regra}" com selo "Pendente" ou "Não verificável" (texto + ícone), "Quem resolve: você" ou "Quem resolve: a unidade emissora". Botão: "Ver detalhes". |
| Requer conferência | "Precisa da sua conferência." / "Não conseguimos ler algumas informações. Isso não significa que elas estejam faltando." Botões: "Conferir leitura", "Enviar arquivo mais nítido". |
| Sem pendências identificadas | "Sem pendências identificadas." / "Não encontramos pendências nos critérios de teste. Isso não é aprovação do SUS, não garante vaga e não define prazo." Botão: "Concluir simulação". |
| Falha na análise | "A conferência não pôde ser feita." / "Nenhuma conclusão foi tirada. Você pode tentar de novo." Botão: "Tentar de novo". |

### E3.4 Detalhe da pendência
| Estado | Textos |
|---|---|
| Reenviar | Título: "{regra}". "Critério: {descrição}". "Protocolo: PROTOCOLO-ACADEMICO-FICTICIO v0.1 (de teste)". "Trecho de origem: {trecho}" (texto puro, ex.: `Dr. A & B <teste> 10 < 20`). "O que fazer: {mensagem do protocolo}". "Quem resolve: você". Para foto cortada ou arquivo ausente: "Envie o arquivo de novo, inteiro e nítido." Botão: "Reenviar arquivo". |
| Procurar a unidade emissora | Título: "{regra}". "O que fazer: O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento." "Quem resolve: a unidade emissora". Texto: "Você não pode preencher esta informação no aplicativo." Botão: "Voltar ao resultado". |

### E3.5 Reenvio e comparação
| Estado | Textos |
|---|---|
| Antes do novo resultado | "Arquivo reenviado." / "Estamos conferindo de novo. Nenhuma pendência é marcada como resolvida até o novo resultado." |
| Comparação | Título: "Antes e depois". Colunas: "Pendência anterior" e "Agora". Valores: "Resolvida" ou "Ainda pendente", sempre com texto + ícone. |

### E3.6 Comprovante da simulação
| Estado | Textos |
|---|---|
| Padrão | Destaque: "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS". "Referência: {referência}", "Data: {data e hora}", "Serviço: {serviço}", "Versão analisada: revisão {n} · PROTOCOLO-ACADEMICO-FICTICIO v0.1". Nota: "Este comprovante é de demonstração. Não é documento clínico, não é protocolo oficial e não coloca você em fila real." |
| Operação repetida | Faixa: "Esta simulação já foi concluída. Mostramos o mesmo comprovante." + o mesmo conteúdo. |

---

## E4 — Sensores, administrador e conexão ruim

### E4.1 Estação de captura
| Estado | Textos |
|---|---|
| Normal | Título: "Condições da captura". "Luminosidade: {lux} lux — Adequada" e "Distância: {cm} cm — Adequada" (texto + ícone). "Leitura há {tempo}". Nota: "Valores de demonstração. Eles não comprovam que o documento ficará legível." |
| Luminosidade fora da faixa | "Luminosidade: {lux} lux — Baixa." / "Melhore a iluminação e tente de novo." |
| Distância fora da faixa | "Distância: {cm} cm — Fora do ideal." / "Aproxime ou afaste o celular do documento." |
| Recuperada | "Condições adequadas de novo." (o aviso anterior some; aparece "Tudo certo para fotografar.") |
| Leitura desatualizada | "Leitura de há {tempo}. Pode estar desatualizada." Selo "Desatualizada". |
| Sensores indisponíveis | "Sensores indisponíveis agora." / "Você pode anexar e salvar seus documentos normalmente." |

### E4.2 Visão do administrador
| Estado | Textos |
|---|---|
| Padrão | Título: "Protocolo e indicadores". "Protocolo ativo: PROTOCOLO-ACADEMICO-FICTICIO v0.1 · 6 regras" e lista das regras (código e nome). "Total de pedidos: {n}". "Análises por resultado: sem pendências identificadas {n}, com pendências {n}, requer conferência {n}, falha {n}". "Regras com mais pendências: {regra} ({n})". Nota: "Dados agregados de demonstração. Documentos e pedidos individuais não são exibidos." |
| Carregando | Esqueleto; "Carregando indicadores…" |
| Sem permissão | "Esta visão é só para o administrador." Botão: "Voltar". |

### E4.3 Conexão ruim
| Estado | Textos |
|---|---|
| Tempo esgotado | "A conexão demorou demais." / "O que você digitou foi mantido. Tente de novo." Botão: "Tentar novamente". Texto: "Nada foi enviado até recebermos a confirmação." |
| Sem conexão | "Sem conexão com a internet." / "O que você digitou foi mantido. Se for preciso, escolha o arquivo de novo depois de reconectar." Botão: "Tentar novamente". |

### E4.4 Guia de acessibilidade
Anotações do quadro: metas de `acessibilidade.md`; no quadro de fonte ampliada, o texto de apoio: "Fonte ampliada: o conteúdo essencial continua visível."
