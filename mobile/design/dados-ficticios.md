# Dados fictícios das telas

> **Dados 100% sintéticos.** Nenhum nome, documento, e-mail ou número abaixo pertence a pessoa real. Protocolo acadêmico fictício, não é exigência oficial. Usar **somente** estes dados nos desenhos, para que as telas contem a mesma história.

## Recorte
- Localidade: **Município Exemplo** (fictício).
- Serviço: **Consulta ambulatorial em cardiologia** (contexto documental; as regras não avaliam conteúdo clínico).
- Protocolo: `PROTOCOLO-ACADEMICO-FICTICIO v0.1` (fonte: [docs/protocolo-academico.md](../../docs/protocolo-academico.md)).
- Horário de referência: Salvador. Datas de exemplo em novembro de 2026.

## Pessoas
| Papel | Nome fictício | E-mail fictício | Uso |
|---|---|---|---|
| Paciente A | Ana Exemplo | `paciente.a@exemplo.invalid` | personagem principal de todos os percursos |
| Paciente B | Bruno Exemplo | `paciente.b@exemplo.invalid` | só aparece em telas de permissão (outra paciente) |
| Administrador | Administração Exemplo | `admin@exemplo.invalid` | visão do administrador |

Nunca desenhar senha, token, chave nem hash. Campos de senha mostram apenas pontos.

## Pedidos da paciente A (um por estado)
| Código do pedido | Serviço | Estado | Revisão | Última atualização | Próxima ação (texto do detalhe) |
|---|---|---|---|---|---|
| PED-0001 | Consulta ambulatorial em cardiologia | `RASCUNHO` | 1 | 12/11/2026 09:10 | Anexe os documentos e envie para conferência |
| PED-0002 | Consulta ambulatorial em cardiologia | `PROCESSANDO` | 2 | 12/11/2026 10:02 | Aguarde: a conferência ainda não terminou |
| PED-0003 | Consulta ambulatorial em cardiologia | `REQUER_CONFERENCIA` | 2 | 12/11/2026 10:05 | Confira a leitura dos documentos |
| PED-0004 | Consulta ambulatorial em cardiologia | `COM_PENDENCIAS` | 2 | 12/11/2026 10:06 | Veja as pendências e providencie as correções |
| PED-0005 | Consulta ambulatorial em cardiologia | `SEM_PENDENCIAS_IDENTIFICADAS` | 3 | 12/11/2026 11:30 | Você pode concluir a simulação |
| PED-0006 | Consulta ambulatorial em cardiologia | `FALHA_ANALISE` | 2 | 12/11/2026 10:08 | A conferência falhou. Tente de novo |
| PED-0007 | Consulta ambulatorial em cardiologia | `SUBMETIDA_SIMULACAO` | 3 | 12/11/2026 11:45 | Simulação concluída. Consulte o comprovante |

Um pedido inexistente (para a tela "item não encontrado"): `PED-9999`.

## Documentos
| Arquivo | Tipo | Tamanho | Páginas | Observação |
|---|---|---|---|---|
| `encaminhamento-sintetico.pdf` | PDF | 120 KB | 2 | encaminhamento com texto |
| `cartao-sus-sintetico.jpg` | JPEG | 1,4 MB | 1 | foto do cartão (na versão com falha, aparece cortada) |
| `autorizacao-responsavel-sintetica.png` | PNG | 800 KB | 1 | usado só em exemplo de arquivo extra |
| `nome-de-arquivo-muito-longo-para-testar-o-truncamento-na-interface-do-aplicativo-sintetico.pdf` | PDF | 90 KB | 1 | testar truncamento do nome |

Estados do envio mostrados: aguardando, enviando, enviado, falhou.

## Campos extraídos (leitura SIMULADA)
| Campo | Valor de exemplo | Origem | Status |
|---|---|---|---|
| Nome | Ana Exemplo | `encaminhamento-sintetico.pdf`, página 1 | identificado |
| Data de nascimento | 03/05/1990 | `cartao-sus-sintetico.jpg`, página 1 | identificado |
| Data de emissão | 20/09/2026 | `encaminhamento-sintetico.pdf`, página 1 | identificado |
| Motivo do encaminhamento | (vazio) | `encaminhamento-sintetico.pdf`, página 2 | não localizado |
| Número do cartão | (ilegível) | `cartao-sus-sintetico.jpg`, página 1 | ilegível |

## Pendências do PED-0004 (por regra do protocolo v0.1)
| Regra | Resultado | Texto à paciente (do protocolo) | Responsável | Trecho de origem |
|---|---|---|---|---|
| RC-R01 Encaminhamento presente | ATENDE | — | — | `encaminhamento-sintetico.pdf`, p. 1 |
| RC-R02 Identificação coerente | PENDENTE | "Nome ou data de nascimento diferem entre os documentos. Confira os dados." | Paciente | nome "Ana Exemplo" × "Ana E. Silva" |
| RC-R03 Documento de identificação | NÃO VERIFICÁVEL | "Não conseguimos ler esta informação no documento enviado. Isso não significa que ela esteja faltando. Envie uma foto ou arquivo mais nítido ou confira a transcrição." | Paciente | `cartao-sus-sintetico.jpg` |
| RC-R04 Motivo informado | PENDENTE | "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento." | Unidade emissora | `encaminhamento-sintetico.pdf`, p. 2 |
| RC-R05 Dentro da validade | ATENDE | — | — | emissão em 20/09/2026 |
| RC-R06 Autorização do responsável | NÃO APLICÁVEL | — | — | paciente adulta |

Para o PED-0005 (sem pendências identificadas), todas as regras aplicáveis estão ATENDE e RC-R06 é NÃO APLICÁVEL.

## Resumo simulado (exemplo, sempre com o selo SIMULADO)
> "Resumo simulado: faltam 2 itens para sua conferência. O nome ou a data de nascimento diferem entre os documentos (confira os dados). O encaminhamento não traz o motivo (peça à unidade emissora que complete o documento). Este resumo é de demonstração."

Exemplo com símbolos (para testar texto puro): trecho de origem `Dr. A & B <teste> 10 < 20` exibido exatamente assim, como texto.

## Comparação do reenvio (PED-0004 → revisão 3)
| Pendência anterior | Depois do novo resultado |
|---|---|
| RC-R02 Identificação coerente | Resolvida |
| RC-R03 Documento de identificação | Resolvida |
| RC-R04 Motivo informado | Ainda pendente (unidade emissora) |

Antes do novo resultado, nada aparece como "resolvida".

## Comprovante simulado (PED-0007)
| Item | Valor |
|---|---|
| Referência interna | SIM-2026-000001 |
| Data | 12/11/2026 11:45 |
| Serviço | Consulta ambulatorial em cardiologia (Município Exemplo, fictício) |
| Versão analisada | revisão 3 · `PROTOCOLO-ACADEMICO-FICTICIO v0.1` |
| Destaque obrigatório | SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS |

## Sensores
| Situação | Luminosidade | Distância | Idade da leitura |
|---|---|---|---|
| Normal | 420 lux | 25 cm | há 8 s |
| Pouca luz | 60 lux | 25 cm | há 5 s |
| Muito perto/longe | 420 lux | 8 cm ou 70 cm | há 5 s |
| Recuperada | 380 lux | 27 cm | há 4 s |
| Desatualizada | 420 lux | 25 cm | há 3 min |
| Indisponível | — | — | sem leitura |

Faixas demonstrativas (ilustrativas, não comprovam legibilidade do documento): luminosidade entre 200 e 1000 lux; distância entre 15 e 40 cm.

## Administrador (agregados fictícios)
| Indicador | Valor |
|---|---|
| Total de pedidos | 24 |
| Análises: sem pendências identificadas / com pendências / requer conferência / falha | 6 / 11 / 5 / 2 |
| Regras com mais pendências | RC-R02 (7), RC-R04 (5), RC-R03 (4) |
| Protocolo ativo | `PROTOCOLO-ACADEMICO-FICTICIO v0.1` · 6 regras |

A visão do administrador não mostra nome, e-mail, documento nem código de pedido de paciente.
