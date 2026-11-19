# Contrato: verificação antes e depois

| Verificação | Antes | Depois | Critério |
|---|---|---|---|
| Build e testes (`backend/`, `.\mvnw.cmd clean verify`) | resultado e tempo registrados | idem | mesmo resultado; mediana de 3 execuções ≤ +5%, avaliada contra o ruído medido (SC-005) |
| `docs/api/openapi.yaml` e coleção Postman | hash | hash | idêntico, salvo correção documental justificada e registrada (SC-007) |
| Links relativos e caminhos citados | contagem de quebrados | zero | SC-002 |
| Índices | órfãos e entradas inexistentes | zero | SC-002 |
| Convenções C1 a C12 | desvios por documento | zero | SC-003 |
| Duplicação (parágrafos ≥ 3 linhas repetidos) | linhas redundantes | redução ≥ 20% (fórmula: linhas em parágrafos duplicados ÷ total de linhas dos documentos mantidos) | SC-004 |
| Casos de teste no plano | 94 | 94 | nenhum caso perdido |
| Diff em `backend/src/main` | n/a | somente comentários/JavaDoc ou código morto comprovado | FR-010 |
| Amostra de temas (≥ 15) | n/a | uma definição completa cada | SC-001 |

Resultados reais são registrados com comando e saída; verificação não executada é declarada como não executada.
