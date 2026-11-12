# Arquitetura

Monolito modular em Java puro: um processo, um módulo Maven (`backend/`), pacotes por funcionalidade em `br.com.regulacheck`, cada um com as camadas controller/service/repository/entity/dto conforme necessário. Cada pacote ainda vazio tem um `package-info.java` descrevendo sua finalidade; não há classes de negócio.

## Tecnologia (exigência da disciplina, decisão de 12/11/2026)
- **Sem Spring e sem JPA/Hibernate ORM no código de produção.** Exceção: Spring Boot pode ser usado apenas em testes automatizados (escopo `test` no Maven). A camada HTTP usa Jakarta Servlet em servidor embutido (Jetty ou Tomcat); os controllers são servlets.
- **Banco Oracle fornecido pela faculdade**, acessado por JDBC com SQL parametrizado. Os repositories executam o SQL diretamente; o esquema vem dos scripts em `database/scripts/`.
- Sem container de injeção de dependência: as dependências são montadas manualmente por construtor em uma classe de composição (`config`).
- **Telemetria IoT:** o script em `simulador-iot/` simula os dois sensores e envia leituras em JSON por HTTP POST; o módulo `iot` do backend valida (credencial, sessão de captura, faixa, atualidade) e persiste. Sem broker de mensagens.
- **Migração pendente:** o código atual ainda usa Spring Boot 4.1.2, JPA e PostgreSQL. Ver [decisoes.md](decisoes.md).

## Módulos
config, auth, usuario, solicitacao, documento, extracao, protocolo, analise, validacao (model, regra), integracao/ia, integracao/extracao, armazenamento, iot, submissao, indicador, common/exception.

## Regras de dependência
- Controller -> Service (nunca Repository). Service -> Repository, motor de regras, contratos de integração.
- Repository não chama Controller, Service nem IA.
- Regras documentais não dependem de HTTP, JDBC ou SDK de IA.
- Adaptadores externos não decidem o estado de negócio.
- IA explica resultados; não substitui o motor determinístico.
- Entidades não são expostas na API (usar DTOs). Nenhuma credencial vai ao mobile.

## Estados da solicitação (propostos)
RASCUNHO, PROCESSANDO, REQUER_CONFERENCIA, COM_PENDENCIAS, SEM_PENDENCIAS_IDENTIFICADAS, FALHA_ANALISE, SUBMETIDA_SIMULACAO.

Transições previstas: RASCUNHO -> PROCESSANDO -> (REQUER_CONFERENCIA | COM_PENDENCIAS | SEM_PENDENCIAS_IDENTIFICADAS | FALHA_ANALISE); após correção/reenvio volta a PROCESSANDO; somente SEM_PENDENCIAS_IDENTIFICADAS -> SUBMETIDA_SIMULACAO. O estado do resumo de IA é distinto do estado da validação. Implementação futura.

## Contratos
Ver [api/README.md](api/README.md) e [../CONTRATO_API.md](../CONTRATO_API.md). A conferência da transcrição é separada de alteração clínica do documento original; informação clínica ausente é corrigida pela unidade emissora. O sistema não diagnostica, não define urgência nem autoriza atendimento; a submissão é simulada.
