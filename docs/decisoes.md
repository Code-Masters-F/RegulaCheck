# Decisões

## Adotadas
| Item | Decisão |
|---|---|
| Linguagem/framework | Java **sem Spring e sem JPA** em produção (exigência da disciplina, 12/11/2026); Spring Boot permitido **somente em testes automatizados** (escopo `test`); web com Jakarta Servlet + Jetty/Tomcat embutido. **Migração pendente**: o código atual ainda usa Spring Boot 4.1.2 |
| Java | `java.version=21` (proposta do documento); compila e roda no JDK 25.0.2 (Temurin) instalado |
| Build | Maven Wrapper 3.3.4 -> Maven 3.9.14 |
| Dependências | web, validation, actuator, webmvc-test (Boot 4 separou os módulos de teste) |
| Health | Apenas `/actuator/health`, sem detalhes |
| Spring Security | **Adiado**. Não há endpoints de negócio; base apenas local. |
| Banco | **Oracle** via JDBC (exigência da disciplina, 12/11/2026), SQL parametrizado, sem ORM. Instância **fornecida pela faculdade** (sem Oracle local em Docker). **Migração pendente**: o ambiente atual ainda usa PostgreSQL 18.6 em Docker (porta 5440) com Spring Data JPA; scripts SQL precisam ser portados para Oracle |
| IoT | 2 sensores **simulados por script**, enviando **JSON por HTTP** ao backend (decisão de 12/11/2026); MQTT descartado. Script em **Java** (`java.net.http.HttpClient`) |
| IA e extração | **Sem provedor real**: resumo de pendências e extração/OCR com dados mockados (decisão de 12/11/2026), atrás de contrato/adaptador e identificado como simulado na interface e documentação |
| Recorte e protocolo | Município Exemplo (fictício), cardiologia ambulatorial, `PROTOCOLO-ACADEMICO-FICTICIO v0.1` com 6 regras; valores **propostos, a confirmar pelo grupo**. Ver [protocolo-academico.md](protocolo-academico.md) e [escopo-e-limitacoes.md](escopo-e-limitacoes.md) |
| Mobile | **Android nativo com Kotlin** (confirmado) |
| Spring Boot | Versão recomendada pelo Spring Initializr em 11/11/2026 (4.1.2) |

## Pendências
- Autenticação/Security e documentação OpenAPI.
- Dados de conexão do Oracle da faculdade (host, porta, service name/SID, usuário, permissões de DDL, rede/VPN necessária): obter com a faculdade e guardar fora do repositório (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Versão do Oracle e do driver JDBC (ojdbc) compatível; confirmar antes de fixar no `pom.xml`.
