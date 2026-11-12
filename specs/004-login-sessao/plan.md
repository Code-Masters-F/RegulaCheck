# Implementation Plan: Login e encerramento de sessão (RC-04)

**Branch**: `004-login-sessao` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-login-sessao/spec.md`

## Summary

Primeira funcionalidade de negócio do backend: `POST /auth/login`, `GET /auth/sessao` e `POST /auth/logout`. A senha é verificada com BCrypt (biblioteca `at.favre.lib:bcrypt`) contra os hashes `$2a$12$…` já carregados pela RC-03. A sessão é um token aleatório de 256 bits entregue à paciente; o banco guarda apenas o SHA-256 do token numa nova tabela aditiva `RC_SESSAO_LOGIN` (script `05_sessao_login.sql`), com prazo fixo de 30 minutos. Um filtro reutilizável (`AutenticacaoFiltro`) valida o token e identifica a conta, para uso das próximas histórias. Esta é também a primeira história a usar JDBC: entram o driver Oracle e um pool de conexões, sem exigir banco para iniciar a API. A tela Android **não** faz parte; seu comportamento fica documentado no contrato.

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release` já em 21; JDK de desenvolvimento 25 funciona).

**Primary Dependencies** (novas, versões fixas, todas a resolver e confirmar no build): `at.favre.lib:bcrypt` 0.10.2 (traz `at.favre.lib:bytes` 1.6.1), `com.oracle.database.jdbc:ojdbc11` 23.26.4.0.0 (versão já verificada contra a faculdade em 12/11/2026), `com.zaxxer:HikariCP` 6.3.0 (pool). Já existentes: Jetty 12 EE10 Servlet, Jackson, SLF4J, JUnit.

**Storage**: Oracle 19c da faculdade via JDBC com SQL parametrizado; nova tabela `RC_SESSAO_LOGIN` criada por script aditivo. Contas lidas de `RC_USUARIO` (RC-03).

**Testing**: JUnit 5. (1) Unitários e de servlet em Jetty real com repositórios em memória, sem banco, no `mvnw clean verify`. (2) Integração opt-in contra o Oracle real (exige `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` e `RC_TESTE_ORACLE=SIM`; sem isso é ignorada e registrada como não executada). (3) Teste de compatibilidade que lê os 3 hashes de `02_carga_sintetica.sql` e verifica a senha de demonstração com a biblioteca, sem banco.

**Target Platform**: JVM 21+ em Windows/Linux; cliente futuro: app Android.

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: login ≤ 1 s no hardware de desenvolvimento (BCrypt custo 12 leva centenas de ms por desenho); validação de sessão numa consulta indexada. Sem metas de carga (volume de demonstração).

**Constraints**: sem Spring/JPA; segredos só em variáveis de ambiente; nenhum log com senha, hash ou token; a API **inicia sem banco** (`/health` continua independente); respostas de erro no formato padrão; identificadores Oracle ≤ 30 caracteres, prefixo `RC_`; schema nunca alterado de forma destrutiva.

**Scale/Scope**: 3 rotas, 1 tabela nova, 1 filtro reutilizável, ~15 classes; 3 contas de demonstração.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: só as 3 contas fictícias da RC-03 (`*.invalid`); nenhum dado real. |
| II. Limites clínicos | Atende: login não toca conteúdo clínico, diagnóstico, urgência nem autorização. |
| III. Monólito modular Java sem Spring/JPA | Atende: Servlet/Jetty + JDBC puro. Controller → Service → Repository; Controller não acessa Repository. Repositórios são interfaces por serem fronteira externa (banco) substituível em teste. DTOs separados da entidade. |
| IV. Segurança e autorização | Atende: BCrypt de biblioteca consolidada; SQL parametrizado; token imprevisível, só o hash no banco; paciente identificada pela sessão, nunca por id no corpo; sem segredo/senha/token em log ou resposta. Autorização por recurso e perfis são da RC-05 (declarado fora do escopo). |
| V. Regras determinísticas / IA | N/A: nenhuma regra documental nem IA. |
| VI. Persistência e confiabilidade | Atende: sucesso do login só depois de gravar a sessão; logout idempotente em efeito; script SQL aditivo, sem DDL destrutivo (único `DROP` continua no `90_`, que passa a incluir a nova tabela). |
| VII. Verificação honesta | Atende: testes de servlet sem banco e integração Oracle opt-in; o que não rodar será registrado como não verificado. |
| VIII. Código documentado, mudanças mínimas | Atende: JavaDoc em português; só as classes necessárias; sem código que finja funcionalidade. |
| Restrições tecnológicas | Atende: nenhum banco trocado; `DB_*` continuam **não obrigatórias** para iniciar (pool preguiçoso, falha só em rotas que usam banco, com 503). |

**Re-avaliação após o desenho (Phase 1)**: sem violações novas. Uma justificativa de dependência extra está em *Complexity Tracking*.

## Project Structure

### Documentation (this feature)

```text
specs/004-login-sessao/
├── plan.md              # Este arquivo
├── research.md          # Decisões D1–D14
├── data-model.md        # RC_SESSAO_LOGIN e tipos de domínio
├── quickstart.md        # Como verificar login/sessão/logout
├── contracts/
│   └── api-auth.md      # Rotas, corpos, erros e comportamento do cliente
├── checklists/
│   └── requirements.md
└── tasks.md             # Gerado por /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── pom.xml                                            # + ojdbc11, HikariCP, bcrypt
└── src/
    ├── main/java/br/com/regulacheck/
    │   ├── config/
    │   │   ├── Configuracao.java                      # + banco (DB_*) e SESSION_TTL_MINUTES
    │   │   ├── BancoDados.java                        # novo: DataSource preguiçoso (Hikari)
    │   │   ├── Dependencias.java                      # novo: repositórios + Clock injetáveis em testes
    │   │   ├── Servidor.java                          # + criar(Configuracao, Dependencias)
    │   │   └── Composicao.java                        # monta repositórios/serviços/controllers de auth
    │   ├── common/
    │   │   ├── Json.java                              # + leitura de corpo JSON
    │   │   └── http/ServletJson.java                  # + leitura de corpo limitada
    │   ├── usuario/
    │   │   ├── entity/Usuario.java                    # id, email, nome, senhaHash, perfil, ativo
    │   │   └── repository/{UsuarioRepository,UsuarioRepositoryJdbc}.java
    │   └── auth/
    │       ├── entity/Sessao.java
    │       ├── repository/{SessaoRepository,SessaoRepositoryJdbc}.java
    │       ├── service/{AutenticacaoService,VerificadorSenha,GeradorToken}.java
    │       ├── dto/{LoginRequest,LoginResponse,SessaoResponse}.java
    │       └── controller/{LoginController,SessaoController,LogoutController,AutenticacaoFiltro,SessaoAutenticada}.java
    └── test/java/br/com/regulacheck/auth/             # unitários, servlet (Jetty) e Oracle opt-in
database/scripts/
├── 05_sessao_login.sql                                # novo, aditivo
├── 90_limpeza_DESTRUTIVA.sql                          # inclui RC_SESSAO_LOGIN (guarda inalterada)
└── README.md                                          # ordem e efeito do 05
docs/
├── api/{erros.md,openapi.yaml,contratos-dados.md,README.md,RegulaCheck.postman_collection.json}
├── modelo-de-dados.md, arquitetura.md, decisoes.md, ambiente.md
CONTRATO_API.md, mobile/README.md, .env.example
```

**Structure Decision**: segue a árvore de pacotes de `CLAUDE.md` §6 (`auth`, `usuario`) e acrescenta `auth/repository` e `auth/entity`, que a tabela prevê só para `usuario`; a sessão pertence a `auth`, então fica lá. Nenhum módulo Maven novo.

## Complexity Tracking

| Desvio | Por que é necessário | Alternativa mais simples rejeitada porque |
|---|---|---|
| Dependência extra **HikariCP** (além do driver e do BCrypt, que são exigidos) | Cada requisição protegida consulta a sessão; abrir conexão Oracle nova custa centenas de ms e a conta da faculdade tem limite de sessões. Um pool pequeno (máx. 3) resolve e permite iniciar sem banco. | Abrir uma conexão por requisição: lento e arriscado para o banco compartilhado. Pool escrito à mão: código de concorrência frágil que a história não deve carregar. |
