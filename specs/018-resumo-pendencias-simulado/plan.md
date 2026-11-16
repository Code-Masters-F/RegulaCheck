# Implementation Plan: RC-14 resumo simulado

**Branch**: `017-app-android-percurso-minimo` (trabalho alheio preservado) | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

## Technical Context

Java 21, Jakarta Servlet, JDBC Oracle, Maven Wrapper; monólito modular. A RC-13 expõe histórico e análise vigente. `RC_ANALISE` possui `RESUMO_TEXTO` e `RESUMO_ORIGEM`; nenhum provedor externo é usado. A geração usa pendências gravadas e textos fixos do protocolo, nunca valores ou trechos do documento.

## Constitution Check

Java puro sem Spring em produção; Oracle JDBC; dados sintéticos; protocolo fictício; sem diagnóstico, prescrição ou promessa. Autorização por pedido antes de leitura e escrita. A saída simulada passa pelo mesmo validador que receberia eventual outro adaptador. Consulta e gravação respeitam análise vigente e não alteram o motor. Sem novas dependências. **Gate: aprovado.**

## Phase 0: Research

Ver [research.md](research.md). Reutilizar colunas existentes. O texto geral tem limite de 2000 caracteres; itens estruturados são derivados das pendências e verificados contra códigos gravados. Fallback é persistido com origem `PADRAO`, mas nova tentativa pode substituí-lo.

## Phase 1: Design & Contracts

Ver [data-model.md](data-model.md), [contracts/api-resumo.md](contracts/api-resumo.md) e [quickstart.md](quickstart.md). `POST /solicitacoes/{id}/resumo` gera ou retorna resumo vigente; `GET /solicitacoes/{id}/analises` inclui resumo nas análises que o possuem. Gravação condicionada à vigência sob bloqueio do pedido.

## Structure

`backend/src/main/java/br/com/regulacheck/resumo/` contém contrato, adaptador mockado, validação, serviço e controller; `analise/repository` persiste e `analise/dto` inclui o resumo. `database/scripts/01_criar_esquema.sql` já possui as colunas. Verificação em `backend/src/test/java/br/com/regulacheck/analise/`.

## Post-design Constitution Check

Sem desvios. Campos clínicos livres ficam fora da entrada do adaptador; somente códigos e mensagens fixas do protocolo compõem a orientação.
