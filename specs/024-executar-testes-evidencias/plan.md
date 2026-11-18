# Implementation Plan: Executar testes e registrar evidências (RC-25)

**Branch**: `024-executar-testes-evidencias` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Input**: `specs/024-executar-testes-evidencias/spec.md` e o plano de testes `docs/testes/plano-de-testes.md` (RC-24, PR #77, 94 casos).

## Summary

Executar o plano de testes da RC-24 contra a versão atual da `main` e transformar cada execução em evidência individual em `docs/evidencias/<ID-DO-CASO>/`, preenchendo no plano resultado, status, evidência, responsável e data. Não há funcionalidade nova: o código só muda para corrigir **defeito bloqueador**, e então só os casos afetados e o P01 são repetidos.

Abordagem em ondas, da mais barata e independente à mais dependente de dispositivo:

0. **Preparação**: congelar a versão testada (commit), conferir ambiente, `mvnw clean verify` e regressão dos conjuntos como linha de base. Baseline automática **não aprova caso** por si só.
1. **API real + Oracle** (curl): T01, T02, T04–T14, T15 (parte API). Um pedido novo por caso.
2. **P01**: percurso integrado (conjuntos 09 → 14) em API real; repetido no app se houver emulador.
3. **Dublê** (T12, T05-03, T04-04, T11-05): testes automatizados existentes, registrados como "Dublê (não é integração real)".
4. **App no emulador** (T01-07, T15-06..09 e casos "App no emulador"): `adb`, JDK 21 do Android Studio só na sessão.
5. **Sensores** (T16): simulador Java; alertas na UI e T03 são tentados e, se o impedimento persistir, marcados Bloqueado.
6. **Correção e reteste** dos bloqueadores; **consolidação** (resumo da rodada, índices).

## Technical Context

**Language/Version**: Markdown e comandos de shell (curl, `adb`, `java`, Maven/Gradle Wrapper). Código-fonte só se um bloqueador exigir (Java 21+ no backend; Kotlin no app).

**Primary Dependencies**: nenhuma nova. Ambiente do plano 5.1: API (jar sombreado em `backend/target/`), Oracle do grupo (`DB_*`), `DOCUMENT_STORAGE_PATH` fora do repositório, `IOT_API_KEY`, emulador Android, simulador `simulador-iot/SimuladorSensores.java`.

**Storage**: arquivos em `docs/evidencias/<ID>/` e atualização de `docs/testes/plano-de-testes.md`; o banco recebe só pedidos de teste criados pelos casos (sem limpeza destrutiva).

**Testing**: execução manual por HTTP e pelo app, mais os testes automatizados citados pelos casos de dublê. Oracle automatizado opt-in (`RC_TESTE_ORACLE=SIM`) só se autorizado.

**Target Platform**: Windows 11 (Git Bash/PowerShell), API local em `http://localhost:8080`, emulador em `http://10.0.2.2:8080`.

**Project Type**: verificação de sistema existente (backend + mobile + simulador).

**Performance Goals**: não se aplica; o tempo do percurso é da RC-26/RC-27.

**Constraints**: dados sintéticos; sem token/senha/chave/`DB_*` em evidência; sem `90_limpeza_DESTRUTIVA.sql` nem reaplicação da carga; sem commit/push/comentário sem autorização; extração e resumo simulados.

**Scale/Scope**: 94 casos, 16 cenários, 1 percurso; mínimo da issue 5 evidências. Prazo da issue (17/11) vencido, execução em 18/11.

### Estado do ambiente nesta sessão (verificado em 18/11/2026)

| Item | Estado | Consequência |
|---|---|---|
| JDK padrão | Temurin 25.0.2 | serve ao backend (requisito 21+); o app exige JDK 21 do Android Studio só na sessão |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | **ausentes** no ambiente do agente | Oracle e API real dependem do responsável iniciar a API (como na RC-19); sem isso os casos ficam Bloqueados após tentativa (503 `DEPENDENCIA_INDISPONIVEL`) |
| `DOCUMENT_STORAGE_PATH`, `IOT_API_KEY` | ausentes | definir a quem sobe a API; sem a primeira, upload responde 503 |
| `adb` | **fora do PATH** | localizar em `ANDROID_HOME`/Android Studio; sem emulador, casos do app ficam Bloqueados após tentativa |
| `curl` | disponível | casos de API executáveis |
| Jar do backend | `backend/target/regulacheck-backend-0.1.0-shaded.jar` existe, mas pode estar defasado | reconstruir com `mvnw clean verify` na versão testada |

Esses itens não são NEEDS CLARIFICATION: viram **gate de ambiente** (Onda 0) e decidem o status Bloqueado quando faltarem.

## Constitution Check

*GATE: passa antes da pesquisa e após o desenho.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Passa: contas `.invalid`, `samples/` sintéticos; aviso de simulação em toda evidência. |
| II. Limites clínicos | Passa: casos reproduzem o contrato; não editam documento original. |
| III. Backend Java em monólito | Passa: nenhuma tecnologia nova; correção, se houver, no padrão existente. |
| IV. Segurança e autorização | Passa: casos T02/T03 exercitam isolamento; evidência sem credenciais; mascarar token. |
| V. Regras determinísticas, IA subordinada | Passa: resumo simulado identificado; instrução em documento (conjunto 11) é dado. |
| VI. Persistência e confiabilidade | Passa: pedido novo por repetição; sem limpeza destrutiva. |
| VII. Verificação honesta | **Aplicação central:** Aprovado só com execução e evidência; Bloqueado só após tentativa; dublê declarado; esperado nunca editado. |
| VIII. Código documentado, mudanças mínimas | Passa: correção só de bloqueador, com JavaDoc/teste e spec atualizada. |

Sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/024-executar-testes-evidencias/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── formato-evidencia-e-registro.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks
```

### Arquivos do repositório afetados

```text
docs/testes/plano-de-testes.md        # preenche resultado, status, evidência, responsável, data; seções 2 e 10
docs/testes/resultado-da-rodada.md    # novo: resumo consolidado (versão, contagens, achados, lacunas)
docs/testes/README.md                 # link para o resumo da rodada
docs/evidencias/<ID-DO-CASO>/         # novo por caso executado: registro.md + bruto/
docs/evidencias/README.md             # índice das pastas da rodada e regra de mascaramento
backend/ ou mobile/                   # SOMENTE se um defeito bloqueador for corrigido (com teste)
```

**Structure Decision**: documentação e evidências em `docs/`; nenhum diretório de código novo. Correções, se necessárias, ficam nos pacotes existentes e entram nesta branch com spec/tarefas atualizadas.

## Complexity Tracking

Sem violações a justificar.
