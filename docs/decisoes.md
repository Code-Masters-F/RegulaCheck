# Decisões

## Adotadas
| Item | Decisão |
|---|---|
| Linguagem/framework | Java **sem Spring e sem JPA** (exigência da disciplina, 12/11/2026); web com Jakarta Servlet + Jetty/Tomcat embutido. **Migração pendente**: o código atual ainda usa Spring Boot 4.1.2 |
| Java | `java.version=21` (proposta do documento); compila e roda no JDK 25.0.2 (Temurin) instalado |
| Build | Maven Wrapper 3.3.4 -> Maven 3.9.14 |
| Dependências | web, validation, actuator, webmvc-test (Boot 4 separou os módulos de teste) |
| Health | Apenas `/actuator/health`, sem detalhes |
| Spring Security | **Adiado**. Não há endpoints de negócio; base apenas local. |
| Banco | **Oracle** via JDBC (exigência da disciplina, 12/11/2026), SQL parametrizado, sem ORM. Instância **fornecida pela faculdade** (sem Oracle local em Docker). **Migração pendente**: o ambiente atual ainda usa PostgreSQL 18.6 em Docker (porta 5440) com Spring Data JPA; scripts SQL precisam ser portados para Oracle |
| IA | **Gemini provável** (não confirmado). Nenhum SDK instalado; variáveis `AI_*` reservadas |
| Mobile | **Android nativo com Kotlin** (confirmado) |
| Spring Boot | Versão recomendada pelo Spring Initializr em 11/11/2026 (4.1.2) |

## Pendências
- Confirmar Gemini e o modelo; provedor/biblioteca de extração (OCR).
- Autenticação/Security e documentação OpenAPI.
- Dados de conexão do Oracle da faculdade (host, porta, service name/SID, usuário, permissões de DDL, rede/VPN necessária): obter com a faculdade e guardar fora do repositório (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Versão do Oracle e do driver JDBC (ojdbc) compatível; confirmar antes de fixar no `pom.xml`.
