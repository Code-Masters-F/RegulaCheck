# Escopo, tecnologias e limitações (RC-01)

Registro do que o demonstrador usa, do que é simulado e do que ele não faz. Decisões completas em [decisoes.md](decisoes.md); regras do protocolo em [protocolo-academico.md](protocolo-academico.md). Situação em 12/11/2026.

## Tecnologias

| Item | Status | Observação |
|---|---|---|
| Java sem Spring e sem JPA/Hibernate em produção | Adotada (migração pendente) | Exigência da disciplina. O código atual em `backend/` ainda é Spring Boot 4.1.2/JPA/PostgreSQL e será migrado |
| Spring Boot em testes automatizados | Adotada (somente escopo `test`) | Única exceção permitida |
| Jakarta Servlet + Jetty ou Tomcat embutido | Adotada (a implementar) | Servidor ainda a escolher entre Jetty e Tomcat |
| JDBC com SQL parametrizado, sem ORM | Adotada (a implementar) | Repositories executam o SQL diretamente |
| Banco Oracle da faculdade | Adotada | Dados de conexão pendentes; scripts SQL precisam ser portados do PostgreSQL |
| Maven Wrapper | Adotada | Versões fixadas |
| Mobile Android nativo em Kotlin | Adotada (não criado) | Consome a API; sem acesso a banco ou IA |
| Resumo de pendências por IA | **Simulada** | Dados mockados atrás de contrato e adaptador; sempre identificado como simulado; não é integração real |
| Extração/OCR de documentos | **Simulada** | Dados mockados atrás de contrato e adaptador; sempre identificado como simulado |
| Dois sensores (luminosidade em lux, distância em cm) | **Simulada** | Script Java (`java.net.http.HttpClient`) envia JSON por HTTP à API |
| MQTT | Descartada | Substituído por HTTP com JSON |
| Docker/PostgreSQL local | Descartada para o produto final | Resquício da fundação antiga |
| Provedor de IA real | Descartada | Sem chave nem chamada externa |

## Limitações do protótipo
- Somente dados sintéticos e protocolo acadêmico fictício; não há documentos reais.
- Sem diagnóstico, definição de urgência, autorização de atendimento ou fila real do SUS; a submissão é simulada.
- "Sem pendências identificadas" não é aprovação do SUS, e os resultados de teste não comprovam redução de filas.
- IA e extração/OCR simuladas: o comportamento não reflete o de modelos ou leitores reais.
- Sensores simulados, com faixas demonstrativas e sem calibração real; não comprovam a legibilidade do documento, e a indisponibilidade deles não bloqueia salvamento ou upload.
- A validade de 180 dias do encaminhamento e as demais regras são valores demonstrativos.

## Estado (distinção exigida pela constituição)

| Categoria | Itens |
|---|---|
| Documentado | Recorte, protocolo v0.1, catálogo de regras, decisões de tecnologia |
| Implementado | Fundação técnica antiga (Spring/JPA/PostgreSQL), a ser migrada; nada do negócio |
| Simulado (a implementar) | IA, extração/OCR, sensores |
| Pendente | Migração da stack, conexão Oracle, portabilidade dos scripts SQL, autenticação, mobile |
| Verificação pendente | Aplicação em papel dos 5 casos e confirmação do grupo (ver [protocolo-academico.md](protocolo-academico.md#verificação)) |
| Fora do escopo | Integração real com SUS, IA real, OCR real, microsserviços, mensageria |
