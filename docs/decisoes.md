# Decisões

## Adotadas
| Item | Decisão |
|---|---|
| Linguagem/framework | Java **sem Spring e sem JPA** em produção (exigência da disciplina, 12/11/2026); Spring Boot permitido **somente em testes automatizados** (escopo `test`); web com Jakarta Servlet + Jetty/Tomcat embutido. **Base migrada na RC-02** (antes: Spring Boot 4.1.2) |
| Java | `maven.compiler.release=21` (proposta do documento); compila, testa e roda no JDK 25.0.2 (Temurin) instalado |
| Build | Maven Wrapper 3.3.4 -> Maven 3.9.14 |
| Dependências | `jetty-ee10-servlet` 12.1.15, `jackson-databind` 2.23.0, `slf4j-api`/`slf4j-simple` 2.0.20, `junit-jupiter` 6.1.3 (teste); plugins compiler 3.14.1, surefire 3.6.1, shade 3.6.2; sem Spring Boot nem em teste |
| Health | `GET /health` (200 `{"status":"UP"}`), só estado técnico, não consulta banco |
| Revisões e estados | Após correção ou reenvio a solicitação vai a `PROCESSANDO` com revisão + 1; alteração durante `PROCESSANDO` inicia nova revisão e descarta o resultado antigo; ver [api/estados-e-revisoes.md](api/estados-e-revisoes.md) |
| Acesso do app | Emulador `http://10.0.2.2:8080`; aparelho físico IP do computador; API escuta em `0.0.0.0` por padrão |
| Autenticação e autorização | **Adiadas** (RC-04 e RC-05). A base só tem `GET /health`; os códigos 401 e 403 já estão reservados no contrato de erros |
| Banco | **Oracle** via JDBC (exigência da disciplina, 12/11/2026), SQL parametrizado, sem ORM. Instância **fornecida pela faculdade** (sem Oracle local em Docker). O PostgreSQL/Docker da fundação antiga foi removido na RC-02; os scripts SQL ainda precisam ser criados/portados para Oracle (RC-03) |
| IoT | 2 sensores **simulados por script**, enviando **JSON por HTTP** ao backend (decisão de 12/11/2026); MQTT descartado. Script em **Java** (`java.net.http.HttpClient`) |
| IA e extração | **Sem provedor real**: resumo de pendências e extração/OCR com dados mockados (decisão de 12/11/2026), atrás de contrato/adaptador e identificado como simulado na interface e documentação |
| Recorte e protocolo | Município Exemplo (fictício), cardiologia ambulatorial, `PROTOCOLO-ACADEMICO-FICTICIO v0.1` com 6 regras; valores adotados por decisão do responsável em 12/11/2026, sem validação do grupo. Ver [protocolo-academico.md](protocolo-academico.md) e [escopo-e-limitacoes.md](escopo-e-limitacoes.md) |
| Mobile | **Android nativo com Kotlin** (confirmado) |

## Pendências
- Autenticação e autorização (RC-04/RC-05); OpenAPI e Postman completos conforme as rotas forem implementadas.
- Dados de conexão do Oracle da faculdade (host, porta, service name/SID, usuário, permissões de DDL, rede/VPN necessária): obter com a faculdade e guardar fora do repositório (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Versão do Oracle e do driver JDBC (ojdbc) compatível; confirmar antes de fixar no `pom.xml`.
