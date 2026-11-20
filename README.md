# RegulaCheck

> **Global Solution 2026/2 — CitizenTech (FIAP, 3ESOA).** MVP acadêmico com **dados sintéticos** e **protocolo fictício**; a submissão é **simulada**. O sistema não diagnostica, não define urgência, não autoriza atendimento e não coloca ninguém em fila real do SUS.

## Vídeo pitch

**Link público:** `<PREENCHER: URL do YouTube, Drive ou Vimeo com acesso aberto>` (até 3 minutos; testar em aba anônima antes de entregar).

## Grupo

| RM | Nome |
|---|---|
| 557517 | Guilherme Carneiro Avellar |
| 557577 | João Vitor Oliveira Santos |
| 98493 | Eliseu Fernando Mendes dos Santos |
| 557449 | Victor Martins Ferreira Brandão Ameno |
| 556357 | Mere Angela da Silva |

Nome do grupo: CodeMasters. A lista completa também vai no `.txt` da entrega.

## O problema e o cidadão

**Cidadã beneficiada:** a paciente que precisa enviar um encaminhamento a um serviço de saúde e não sabe se a documentação está completa. Documento faltando ou ilegível só é descoberto depois, e o retorno custa tempo e deslocamento.

**O que o RegulaCheck faz:** a paciente envia os documentos, acompanha a leitura, confere a transcrição, recebe uma avaliação de critérios documentais e um resumo das pendências, corrige, reenvia e faz uma submissão **simulada**. Dois sensores simulados (luminosidade e distância) orientam a captura da foto do documento.

**Limites declarados:** informação clínica errada ou ausente exige revisão pela unidade emissora; a paciente não altera o documento original. "Sem pendências identificadas" não é aprovação do SUS. Não prometemos redução real de filas com base em testes sintéticos.

## ODS da ONU

| ODS | Como o projeto contribui |
|---|---|
| **3 — Saúde e bem-estar** | Reduz idas e voltas por documentação incompleta no acesso a serviços de saúde (meta 3.8, acesso a serviços essenciais). |
| **10 — Redução das desigualdades** | Orienta quem tem menos familiaridade com a burocracia a corrigir pendências sozinha, em linguagem simples. |
| **9 — Indústria, inovação e infraestrutura** | Motor de regras determinístico, leitura de documentos e sensores como apoio à decisão (simulados no MVP). |

## Como o projeto atende às 6 partes da entrega

| Parte | Onde está |
|---|---|
| 1. Banco de dados (Oracle) | Scripts em [`database/scripts/`](database/scripts/README.md): `01_` cria o esquema, `02_` carga inicial, `03_` consultas de uso com JOIN e agregação, `04_` testes de restrições. Modelo em [docs/modelo-de-dados.md](docs/modelo-de-dados.md) |
| 2. API RESTful (Java, Controller/Service/Repository) | [`backend/`](backend/). Documentação: [OpenAPI](docs/api/openapi.yaml) e [coleção Postman](docs/api/RegulaCheck.postman_collection.json). Mapa de rotas em [CONTRATO_API.md](CONTRATO_API.md) |
| 3. Plano de testes | [docs/testes/plano-de-testes.md](docs/testes/plano-de-testes.md), resultados em [docs/testes/resultado-da-rodada.md](docs/testes/resultado-da-rodada.md), evidências em [docs/evidencias/](docs/evidencias/) |
| 4. Frontend mobile (Android/Kotlin) | [`mobile/`](mobile/README.md) |
| 5. Segurança | Senha com BCrypt, perfis `PACIENTE` e `ADMINISTRADOR`, SQL parametrizado, validação de entrada e de arquivos. Ver [docs/api/autorizacao.md](docs/api/autorizacao.md) e [docs/api/protecoes.md](docs/api/protecoes.md) |
| 6. Simulação de IoT | [`simulador-iot/`](simulador-iot/README.md): dois sensores simulados enviam JSON por HTTP à API; o app mostra a orientação |

## Tecnologias

- **Backend:** Java 21+, Jakarta Servlet com Jetty embutido, JDBC. **Sem Spring e sem JPA/Hibernate** em produção (Spring Boot só nos testes automatizados, escopo `test`).
- **Banco:** Oracle (fornecido pela faculdade), SQL parametrizado.
- **Mobile:** Android nativo, Kotlin, Jetpack Compose.
- **IoT:** script Java (`java.net.http.HttpClient`) enviando JSON por HTTP.
- **Leitura de documentos (OCR) e resumo de pendências:** **simulados** (adaptadores mockados atrás de contrato). Não há provedor de IA real em execução e isso é sempre identificado como simulado na interface, na API e na documentação.

## Declaração de uso de IA generativa

| Ferramenta | Para quê foi usada |
|---|---|
| **Claude Code (Anthropic)** | Apoio à implementação do código (backend, app, simulador), dos testes, dos scripts SQL e da documentação, conduzido por especificações e revisado pelo grupo |
| **GitHub Spec Kit** | Fluxo de especificação, plano e tarefas de cada história (diretórios `.specify/` e `specs/`) |

O código e os textos gerados com apoio de IA foram revisados pelo grupo. A IA generativa **não** faz parte do funcionamento do produto: o resumo e a leitura de documentos são simulações determinísticas.

## Executar

Pré-requisito comum: **JDK 21+**. As variáveis ficam no shell ou na IDE; a aplicação **não** lê `.env` sozinha (modelo em [`.env.example`](.env.example)).

### 1. Banco (Oracle)

1. Com o seu cliente SQL (SQL Developer: **F5**, "Executar script"), rode **um único arquivo**: [`rodar_tudo.sql`](database/scripts/rodar_tudo.sql), que executa em ordem [`01_criar_esquema.sql`](database/scripts/01_criar_esquema.sql), [`02_carga_sintetica.sql`](database/scripts/02_carga_sintetica.sql) e [`05_sessao_login.sql`](database/scripts/05_sessao_login.sql) (em banco novo, sem erro). Se preferir, rode esses três arquivos inteiros, um a um, nessa ordem. Opcionais: `03_consultas_uso.sql` (consultas) e `04_testes_restricoes.sql` (testes de restrições, não deixa dados).
2. Os scripts `06_` a `11_` são só para banco criado antes de cada história; o `01_` atual já traz o que eles acrescentam. Detalhes e ordem em [database/scripts/README.md](database/scripts/README.md).
3. Conta e senha do Oracle: pedir ao grupo; **nunca** versionar.
4. Contas de demonstração criadas pela carga: `paciente.a@exemplo.invalid` e `paciente.b@exemplo.invalid` (`PACIENTE`) e `admin@exemplo.invalid` (`ADMINISTRADOR`), todas com a senha fictícia `RegulaCheck#Demo2026`.

### 2. API

Variáveis (PowerShell; em Bash use `export`):

```powershell
$env:DB_URL = 'jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl'
$env:DB_USERNAME = '<usuario>'
$env:DB_PASSWORD = '<senha>'
$env:DOCUMENT_STORAGE_PATH = '<pasta fora do repositório>'   # para enviar documentos
$env:IOT_API_KEY = '<chave qualquer, igual à do simulador>'  # para os sensores
```

A partir de `backend/`:

```powershell
.\mvnw.cmd clean verify
java -jar target\regulacheck-backend.jar
curl http://localhost:8080/health    # {"status":"UP"}
```

Linux/macOS: `./mvnw clean verify` e `java -jar target/regulacheck-backend.jar`. Sem `DB_*`, a API sobe e `/health` responde, mas as rotas de negócio respondem 503. `UP` não prova que o banco está acessível. Os testes contra o Oracle real são opt-in (`RC_TESTE_ORACLE=SIM`); sem isso são ignorados e **não** contam como verificados.

### 3. App Android

Requisitos: Android Studio (SDK 34 e um emulador) e a API no ar. O JDK 25 não serve ao Android Gradle Plugin: use o JDK do Android Studio só nesta sessão.

```powershell
cd mobile\app
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest
adb install -r build\outputs\apk\debug\regulacheck-app-debug.apk
```

No emulador a API é `http://10.0.2.2:8080` (padrão). `localhost` **não** funciona no emulador. Aparelho físico e envio de documentos de exemplo: [mobile/README.md](mobile/README.md) e [docs/ambiente.md](docs/ambiente.md). Entre com `paciente.a@exemplo.invalid`; a conta de administrador não entra no app.

### 4. Sensores simulados

Com a API no ar e uma sessão de captura aberta pela paciente (`POST /solicitacoes/{id}/sessoes-captura`), da raiz do repositório:

```powershell
$env:IOT_API_KEY = '<a mesma chave da API>'
java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia fora-da-faixa
```

Sequências, faixas e códigos de saída em [simulador-iot/README.md](simulador-iot/README.md). As faixas (300–1000 lux, 20–40 cm) são **demonstrativas**, sem calibração; as leituras orientam a captura, não comprovam legibilidade e nunca bloqueiam o envio.

### 5. Documentação da API

Importe [`docs/api/RegulaCheck.postman_collection.json`](docs/api/RegulaCheck.postman_collection.json) no Postman, ou abra [`docs/api/openapi.yaml`](docs/api/openapi.yaml) em um visualizador OpenAPI. A API não usa Spring, então não há Swagger gerado em execução.

## Estado real

| Parte | Situação |
|---|---|
| API (autenticação, pedidos, documentos, leitura simulada, conferência, motor de regras, análise, resumo simulado, reenvio, submissão simulada, sensores) | Implementada; verificada com repositórios em memória e, em 13 a 17/11/2026, em boa parte com o Oracle da faculdade. O que foi verificado em cada história está em `specs/*/tasks.md` |
| App Android | Login, meus pedidos, pedido, envio de documentos, leitura e resultado da análise implementados. Conferência da leitura, substituição/remoção, comprovante da submissão e telas do administrador são **só desenho** (ver [mobile/README.md](mobile/README.md)) |
| Sensores | Implementados e verificados como processo; demonstração ponta a ponta com a API e o Oracle em 19/11/2026 ([evidência](docs/evidencias/IOT-simulacao/registro.md)); `CapturaOracleTest` ainda não executado no Oracle |
| Plano de testes | Plano escrito (94 casos). Em 19/11/2026 foram **executados 10 casos, todos Aprovados**, contra a API real e o Oracle, com evidências em [`docs/evidencias/`](docs/evidencias/) (mais a demonstração dos sensores). Os outros 84 continuam **não executados**; revisão por outro integrante pendente (ver [resultado-da-rodada.md](docs/testes/resultado-da-rodada.md)) |

Detalhes por rota e por história: [docs/api/README.md](docs/api/README.md). Decisões e pendências: [docs/decisoes.md](docs/decisoes.md). Recorte do demonstrador: [docs/protocolo-academico.md](docs/protocolo-academico.md) e [docs/escopo-e-limitacoes.md](docs/escopo-e-limitacoes.md).

## Estrutura

`backend/` API | `mobile/` app | `simulador-iot/` sensores | `database/` SQL | `docs/` documentação | `samples/` documentos sintéticos | `specs/` e `.specify/` Spec Kit | `entrega/` pacote final. Arquitetura em [docs/arquitetura.md](docs/arquitetura.md).
