# Pesquisa e decisões — RC-02

## D1. Servidor embutido: Jetty 12.1.15 (ee10)
- **Decisão**: Jetty (`jetty-ee10-servlet`), com `Server` + `ServletContextHandler` e `addServlet`, sem `web.xml`.
- **Rationale**: documentação oficial mostra o servidor embutido com poucas linhas; API Jakarta EE 10 (`jakarta.servlet`); poucas dependências; exige Java 17+.
- **Alternativas**: Tomcat embutido (válido, mais configuração programática e mais dependências); mantido como alternativa permitida pela constituição.

## D2. JSON: Jackson databind 2.23.0
- **Decisão**: usar Jackson para serializar DTOs e o formato de erro.
- **Rationale**: biblioteca consolidada; DTOs explícitos; sem mapeamento automático de entidades.
- **Alternativas**: montar JSON à mão (frágil e inseguro para escape).

## D3. Logging: SLF4J + implementação simples estável
- **Decisão**: `slf4j-api` 2.0.x com `slf4j-simple` estável; sem `System.out`; sem logar dados sensíveis.
- **Rationale**: Jetty 12 usa SLF4J; evita dependência extra; a alpha 2.1.0 do slf4j-simple não é aceitável (versão estável fixa).

## D4. Testes: JUnit + HttpClient, sem Spring Boot
- **Decisão**: JUnit 6.1.3 (junit-jupiter) e `java.net.http.HttpClient` contra o servidor em porta efêmera (porta 0).
- **Rationale**: Spring Boot em teste é permitido, não obrigatório; evitar a dependência reduz risco e tamanho. Teste de arquitetura verifica que `main` não referencia `org.springframework`, `jakarta.persistence` ou `org.hibernate`.

## D5. Build e JDK
- **Decisão**: `maven.compiler.release=21`; verificar no JDK 25.0.2 já instalado; Maven Wrapper mantido; plugins fixos (compiler, surefire, jar/shade ou assembly para gerar jar executável). Comando de execução documentado após verificação.
- **Rationale**: JDK 21 é a proposta registrada; compila e roda no 25.

## D6. Configuração por variáveis de ambiente
- **Decisão**: `SERVER_PORT` (padrão 8080) e `SERVER_HOST` (padrão `0.0.0.0` para permitir acesso de emulador/aparelho); `DB_*`, `DOCUMENT_STORAGE_PATH`, `IOT_API_KEY` apenas lidas quando existirem, nunca exigidas nesta história. O `.env` não é lido automaticamente.
- **Rationale**: FR-001/FR-006; `0.0.0.0` é necessário para o emulador (`10.0.2.2`) alcançar a API; documentar risco de rede local.

## D7. Health
- **Decisão**: `GET /health` → 200 `{"status":"UP"}`. Não consulta banco nesta história; o texto da documentação diz que UP não comprova que o MVP está completo nem que o banco está acessível.
- **Rationale**: CLAUDE.md §13; sem detalhes.

## D8. Camadas e montagem
- **Decisão**: pacote `saude` com Controller (servlet), Service e contrato `SaudeRepository` + implementação em memória; montagem manual em `config.Composicao`. Servlets sem estado por paciente.
- **Rationale**: demonstra FR-003 sem entidade de negócio; evita DI container.

## D9. Contratos e formato
- **Decisão**: contratos em Markdown (campos, tipos, obrigatoriedade, exemplos fictícios) + `openapi.yaml` escrito à mão cobrindo health, erro e esquemas; rotas de negócio documentadas como "planejadas" até existirem. Postman fica a critério da RC-28.
- **Rationale**: Swagger do Spring não existe mais; o mesmo esquema serve app e API.

## D10. Estados e revisão
- **Decisão**: 7 estados, tabela de transições (contrato `estados-e-revisoes.md`), `revisao` inteira que incrementa a cada alteração relevante (arquivo adicionado/substituído/removido, conferência alterada); análise guarda revisão, versão do protocolo e resultado; publicação condicional por revisão.
- **Rationale**: constituição VI; RC-03 modela o banco a partir disso.

## D11. Endereçamento do emulador
- **Decisão**: emulador Android usa `http://10.0.2.2:8080`; aparelho físico usa `http://<IP-do-computador>:8080` na mesma rede, com firewall liberando a porta; `localhost` não vale. O app em Android 9+ bloqueia HTTP sem TLS por padrão; documentar a configuração de tráfego em texto claro **só para desenvolvimento** (`networkSecurityConfig`) ao criar o app.
- **Rationale**: evita falha comum; HTTPS é exigência para execução remota autenticada (CLAUDE.md §9).

## D12. Migração da fundação antiga
- **Decisão**: remover `spring-boot-starter-*`, `postgresql`, `application.yml`, `database/docker-compose.yml` (PostgreSQL) e testes Spring; manter a árvore de pacotes e o Wrapper. Scripts SQL em `database/scripts/` ficam como estão até a RC-03 (portar para Oracle).
- **Rationale**: o docker-compose é requisito oculto proibido; mover só o necessário; não apagar trabalho alheio sem registro (remoção listada no relatório).

## Estado antes da migração (T001, 2026-11-12)
- `backend/mvnw.cmd -version`: Maven 3.9.14, Java 25.0.2 (Temurin).
- `backend/mvnw.cmd -q clean verify` na fundação antiga (Spring Boot 4.1.2/JPA/PostgreSQL): executou o teste `RegulaCheckApplicationTests` do Spring e terminou sem mensagens de erro (modo silencioso, sem linha "BUILD SUCCESS" capturada).

## Versões de plugins fixadas (T002)
- `maven-compiler-plugin` 3.14.1 (a 4.0.0-beta-5 foi descartada por ser beta), `maven-surefire-plugin` 3.6.1, `maven-shade-plugin` 3.6.2, `slf4j-api` e `slf4j-simple` 2.0.20 (a 2.1.0-alpha1 foi descartada). Jar executável: `backend/target/regulacheck-backend.jar`; comando: `java -jar backend/target/regulacheck-backend.jar`.

## Verificação (T018, 2026-11-12, JDK 25.0.2)
- `backend\mvnw.cmd clean verify`: BUILD SUCCESS; 5 testes, 0 falhas (`ArquiteturaTest` 2, `ErroPadraoTest` 2, `SaudeEndpointTest` 1).
- Observação: os testes foram escritos junto com o código (não houve execução "vermelha" prévia).
- `java -jar target/regulacheck-backend.jar` (sem nenhuma variável): `GET /health` → 200 `{"status":"UP"}`, `application/json`; `GET /nada` → 404 `NAO_ENCONTRADO`; `POST /health` → 405 `METODO_NAO_PERMITIDO` com `Allow: GET`; nenhum stack trace nas respostas.
- Porta 8080 ocupada: mensagem "Nao foi possivel iniciar a API na porta 8080… defina SERVER_PORT"; `SERVER_PORT=abc` → mensagem de valor inválido; `SERVER_PORT=8099` → API sobe e responde UP.
- Limitação conhecida: o Jackson 2.23 emite aviso de depreciação para `serializationInclusion` (funciona e é coberto por testes).

## Verificação do endereçamento (T032, 2026-11-12)
- Verificado: `curl` em `127.0.0.1:8080` e no IP local `172.21.192.1:8080` (interface da máquina de desenvolvimento) → 200 `{"status":"UP"}`; com `SERVER_HOST=127.0.0.1` o IP da rede é recusado (comportamento esperado).
- **Não verificado:** acesso a partir do emulador Android (`10.0.2.2`) e de aparelho físico (não há emulador nem app neste ambiente); o IP `172.21.192.1` parece ser de adaptador virtual, não necessariamente da rede local.

## Conferência dos critérios de sucesso (T035)
| Critério | Situação |
|---|---|
| SC-001 (iniciar só com o README em até 10 min) | Verificado pelo agente com os comandos documentados (build + `java -jar` + curl); não repetido por outra pessoa |
| SC-002 (5 áreas com README) | Verificado: `backend`, `mobile`, `database`, `simulador-iot`, `docs` |
| SC-003 (sem segredos) | Verificado por `git grep` (apenas nomes de variáveis do Maven Wrapper; nenhum `.env` versionado) |
| SC-004 (dois leitores, mesmos campos) | Dispensado por decisão do usuário em 12/11/2026; não verificado |
| SC-005 (tabela de transições sem dúvida) | Revisada pelo agente (T030); verificação por terceiros dispensada por decisão do usuário em 12/11/2026 |
| SC-006 (build e teste de saúde) | Verificado: `BUILD SUCCESS`, 5 testes |
| SC-007 (health pelo emulador) | **PENDENTE**: teste no emulador Android (`http://10.0.2.2:8080/health`) a fazer quando houver emulador e app; mantido em aberto por decisão do usuário |
| SC-008 (RC-03 e RC-04 sem novas definições) | Dispensado por decisão do usuário em 12/11/2026; não verificado |

## Revisão da tabela de transições (T030)
Todo estado é alcançável (RASCUNHO por criação; PROCESSANDO, resultados e FALHA_ANALISE por análise; SUBMETIDA_SIMULACAO por submissão) e todo estado, exceto o final, tem saída. Exclusão de rascunho fica para a RC-09 (fora desta história). Sem contradição com `docs/protocolo-academico.md` e `docs/arquitetura.md` após o alinhamento feito nesta história.
