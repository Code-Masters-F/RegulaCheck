# IOT-simulacao — Dois sensores simulados enviando JSON à API e orientação à paciente

Tipo de execução: API real + Oracle + `simulador-iot/SimuladorSensores.java` (processo real). Leitura e resumo: **simulados**. Sensores **simulados**, faixas **demonstrativas** (sem calibração).

| Item | Valor |
|---|---|
| Data e hora | 2026-11-19 21:55 (America/Bahia) |
| Versão | commit `7532440` (alterações locais só em `docs/`, `README.md`, `entrega/`) · `backend/target/regulacheck-backend.jar` · Oracle da faculdade |
| Responsável | grupo CodeMasters · executado com apoio do Claude Code |
| Pedido / sessão | pedido 153 · sessão de captura 66 (conta A) |
| Resultado bruto | `bruto/` (chave do simulador não registrada) |

## Passos
1. Login da paciente A, `POST /solicitacoes`, `POST /solicitacoes/153/sessoes-captura` (`bruto/01-abrir-sessao.json`).
2. `java simulador-iot/SimuladorSensores.java --sessao 66 --sequencia fora-da-faixa` (com `IOT_API_KEY`) e depois `--sequencia recuperacao` (`bruto/02-simulador-saida.txt`).
3. `GET /solicitacoes/153/leituras` após cada sequência (`bruto/leituras-apos-*.json`).

## Esperado × obtido
- ✔ Sequência `fora-da-faixa`: última luminosidade 110 lux → `ABAIXO_DA_FAIXA`, orientação `MELHORAR_ILUMINACAO`; última distância 58 cm → `ACIMA_DA_FAIXA`, orientação `APROXIMAR_DOCUMENTO`.
- ✔ Sequência `recuperacao`: última luminosidade 650 lux e distância 28 cm → `DENTRO_DA_FAIXA`, orientação `CONDICAO_ADEQUADA` nos dois sensores.
- ✔ A resposta traz `simulada: true` e o aviso de que as leituras não comprovam a legibilidade nem bloqueiam o envio.

## Status final
Aprovado (demonstração de ponta a ponta do simulador à API, ao banco e à orientação) — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19. Não é caso numerado do plano de testes.
