## Cenários mínimos de teste

| ID  | Cenário e entrada                                                     | Resultado esperado                                                               | Histórias      |
| --- | --------------------------------------------------------------------- | -------------------------------------------------------------------------------- | -------------- |
| T01 | Credenciais corretas e incorretas                                     | Sessão válida ou 401, sem vazamento da senha                                     | 04             |
| T02 | Paciente A acessa pedido/arquivo/análise de B                         | Acesso negado e nenhum conteúdo devolvido                                        | 05, 06         |
| T03 | Paciente acessa /admin/indicadores                                    | 403; administrador autorizado recebe agregados                                   | 05, 20         |
| T04 | POST e PUT de rascunho válido; campo obrigatório vazio                | Persistência correta; 400 na entrada inválida                                    | 07             |
| T05 | PDF e JPEG/PNG válidos                                                | Upload e extração reais, com origem dos campos                                   | 08, 10         |
| T06 | Arquivo acima do limite, formato inválido ou PDF protegido            | Rejeição explicada; nenhum resultado de completude                               | 06, 08, 10     |
| T07 | Documento completo segundo gabarito                                   | Critérios atendidos, resumo coerente, sem promessa oficial                       | 11, 12, 14, 15 |
| T08 | Documento com uma ou várias ausências                                 | Pendências previstas, com regra e ação correspondente                            | 12, 14, 15     |
| T09 | Regra condicional aplicável, não aplicável e condição desconhecida    | Exigência, dispensa ou requer conferência, respectivamente                       | 01, 12         |
| T10 | Imagem ilegível ou transcrição marcada como divergente                | Requer conferência; não afirmar requisito ausente com certeza                    | 10, 11, 15     |
| T11 | Documento corrigido reenviado; edição durante análise                 | Nova revisão; resultado antigo não libera pedido atual                           | 09, 13, 16     |
| T12 | IA indisponível, saída inválida e documento com instruções maliciosas | Pendências preservadas; saída inválida rejeitada; instrução ignorada             | 06, 14         |
| T13 | Submissão válida, com pendência, análise antiga e repetida            | Simulação única válida; demais recusadas ou repetição idempotente                | 13, 17         |
| T14 | Excluir rascunho e tentar excluir pedido submetido                    | Exclusão permitida ou conflito, sem acesso indevido                              | 09             |
| T15 | API cai durante upload/análise; sessão expira                         | Feedback, dados recuperáveis e retentativa sem duplicação                        | 04, 08, 13, 19 |
| T16 | Sensores normais, fora da faixa, antigos e sem credencial             | Alertas corretos, indicação de desatualização e rejeição do envio não autorizado | 21, 22         |

Todos começam com status **Não executado**. As variações dentro de cada cenário podem ser separadas em casos individuais no plano final. Acrescentar verificação de divergência de identificação entre documentos se essa regra fizer parte do protocolo escolhido.
