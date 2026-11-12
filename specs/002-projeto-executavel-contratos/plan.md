# Implementation Plan: Projeto executável e contratos (RC-02)

**Branch**: `002-projeto-executavel-contratos` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-projeto-executavel-contratos/spec.md`

## Summary

Migrar a fundação antiga de `backend/` (Spring Boot 4.1.2, JPA, PostgreSQL) para Java puro com Jakarta Servlet em Jetty embutido, sem banco obrigatório, com `GET /health`, exemplo mínimo Controller → Service → Repository, montagem manual das dependências em `config` e erro padronizado. Em paralelo, fixar a estrutura de pastas e os contratos compartilhados: solicitação, upload, análise, pendência, erro, estados/transições e revisão, mais o endereçamento do emulador Android. Os contratos ficam em Markdown e num OpenAPI escrito à mão. Sem entidades de negócio, autenticação, regras, IA ou sensores.

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release=21`); verificado no JDK 25.0.2 (Temurin) instalado.

**Primary Dependencies** (versões fixas, sem SNAPSHOT; checadas no Maven Central em 12/11/2026):
- `org.eclipse.jetty.ee10:jetty-ee10-servlet` 12.1.15 (Jakarta EE 10 Servlet embutido);
- `com.fasterxml.jackson.core:jackson-databind` 2.23.0 (JSON);
- `org.slf4j:slf4j-api` 2.0.x + `slf4j-simple` estável (logging; evitar a alpha 2.1.0-alpha1);
- teste: `org.junit.jupiter:junit-jupiter` 6.1.3.
- **Não** entram agora: driver `ojdbc11` (23.26.4.0.0 é a versão atual; só entra na RC-03 com datasource funcional), Spring Boot em teste (desnecessário; JUnit + `java.net.http.HttpClient` bastam), ORM, pool, biblioteca de hash de senha (RC-04).

**Storage**: Nenhum nesta história. Camada Repository demonstrada com contrato e implementação em memória só para o exemplo mínimo; JDBC/Oracle na RC-03.

**Testing**: JUnit sobre o servidor embutido em porta livre, chamando `GET /health` e rotas inválidas via `HttpClient`; sem banco, sem rede externa. Teste de arquitetura simples para dependências proibidas (nenhum `org.springframework` em `main`).

**Target Platform**: JVM 21+ em Windows/Linux/macOS; API acessível do emulador Android (`10.0.2.2`) e de aparelho físico na mesma rede (IP do computador).

**Project Type**: Web service (monólito modular, um módulo Maven) + pastas reservadas para mobile, simulador e banco.

**Performance Goals**: Inicialização da API em poucos segundos; sem meta de vazão (MVP).

**Constraints**: Nenhuma variável obrigatória para iniciar; `GET /health` só expõe estado técnico; sem stack trace/SQL nos erros; sem segredos no Git; escrita em português-BR e JavaDoc nas classes públicas relevantes.

**Scale/Scope**: 1 endpoint técnico, 5 contratos, 7 estados, ~10 classes Java, 3 documentos novos de contrato.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: exemplos de contrato fictícios; nada real. |
| II. Limites clínicos | Atende: contratos não carregam diagnóstico, urgência nem autorização. |
| III. Backend Java em monólito modular | Atende: Java puro, Servlet embutido, módulo único, camadas separadas; remove Spring/JPA/PostgreSQL de `main`. Spring Boot não é usado nem em teste. |
| IV. Segurança e autorização | Atende parcialmente por escopo: reserva 401/403 e proíbe vazamento; autenticação fica na RC-04/05. Sem segredos versionados. |
| V. Regras determinísticas, IA subordinada | Atende: contratos de análise reutilizam resultados do protocolo e marcam IA/extração como simuladas. |
| VI. Persistência e revisões | Atende no desenho: define revisão, invalidação e publicação condicional; implementação na RC-03/RC-12. Schema não alterado. |
| VII. Verificação honesta | Atende: teste de health real executado e registrado; nada marcado sem execução. |
| VIII. Código documentado, mudanças mínimas | Atende: poucos arquivos, JavaDoc, README/docs atualizados; sem classes vazias que finjam negócio. |
| Fluxo SDD | Atende: specify → plan; seguem tasks → analyze → implement. |

**Resultado**: sem violações; Complexity Tracking não se aplica. A única ressalva é o exemplo mínimo de Repository em memória (Princípio VIII): justificado como demonstração da separação de camadas exigida pelo critério de aceite, sem fingir negócio. Reavaliação pós-design: mantém-se.

## Project Structure

### Documentation (this feature)

```text
specs/002-projeto-executavel-contratos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── solicitacao-upload-analise.md   # contratos de dados (solicitação, upload, análise, pendência)
│   ├── erros.md                        # formato e catálogo de erros
│   ├── estados-e-revisoes.md           # 7 estados, transições, revisão
│   └── openapi.yaml                    # OpenAPI escrito à mão (health + esquemas)
├── checklists/requirements.md
└── tasks.md                            # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── pom.xml                              # reescrito: sem Spring, Jetty/Jackson/JUnit com versões fixas
├── mvnw, mvnw.cmd, .mvn/                # mantidos
├── src/main/java/br/com/regulacheck/
│   ├── RegulaCheckApplication.java      # main: monta dependências e inicia o servidor
│   ├── config/                          # Configuracao (leitura de env com padrões), Composicao (DI manual), Servidor
│   ├── common/exception/                # ErroPadrao, ExcecaoNegocio, tratamento central de erros HTTP
│   ├── saude/                           # exemplo mínimo: SaudeController, SaudeService, SaudeRepository (+ memória)
│   └── (pacotes por funcionalidade existentes, apenas com package-info)
├── src/main/resources/                  # sem application.yml do Spring; logging simples
└── src/test/java/br/com/regulacheck/    # SaudeEndpointTest, ErroPadraoTest, ArquiteturaTest
database/        README.md (Oracle; sem docker-compose do PostgreSQL)
mobile/          README.md (Android Kotlin, endereço do emulador)
simulador-iot/   README.md
docs/            arquitetura.md, ambiente.md, api/ (openapi.yaml, contratos), decisoes.md
.env.example     variáveis (nenhuma obrigatória)
```

**Structure Decision**: manter o módulo Maven único e a árvore `br.com.regulacheck` já existente; trocar apenas o conteúdo técnico. O pacote `saude` é o exemplo mínimo das 3 camadas, sem negócio. A pasta `config` concentra a montagem manual das dependências. Os documentos de contrato ficam na spec e são publicados/espelhados em `docs/api/` na implementação; `CONTRATO_API.md` continua sendo o mapa de rotas.

## Complexity Tracking

Sem violações a justificar.
