# Implementation Plan: Documentar API e respostas HTTP (RC-28)

**Branch**: `55-rc-28-documentar-api-e-respostas-http` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/028-documentar-api-swagger/spec.md`

## Summary

Servir o Swagger UI pela própria API em `GET /docs`, com o contrato escrito à mão (`docs/api/openapi.yaml`, fonte única) empacotado no jar e servido em `GET /docs/openapi.yaml`, para que o avaliador leia e execute as rotas na mesma origem. Completar o contrato com grupos, exemplos fictícios obtidos de execução real, códigos usados (202 declarado como não usado) e falhas de dependência. Escrever o roteiro de avaliação ponta a ponta, verificado por um teste automatizado pelas rotas reais em memória. Corrigir a coleção Postman e os documentos de situação (`GET /admin/indicadores` implementada).

Abordagem técnica (detalhes em [research.md](research.md)):

- Arquivos estáticos do Swagger UI pela webjar `org.webjars:swagger-ui` **5.34.1**, com versão fixa e sem código Java, servidos por um servlet próprio com **lista fixa** de arquivos carregados na subida (falha imediata se faltar algum).
- Página (`index.html`) e inicialização (`inicializar.js`) próprias: layout sem barra de URL, sem validador externo e sem guardar a autorização.
- Política de conteúdo própria **só** nas respostas de sucesso de `/docs`; o filtro global continua igual.
- `/docs/*` entra na lista de caminhos públicos de `RotasPrivadas`.

## Technical Context

**Language/Version**: Java 21 (`maven.compiler.release`), executado com o JDK 25 da máquina de desenvolvimento

**Primary Dependencies**: Jetty 12.1.15 (ee10 servlet), Jackson 2.23.0; nova: `org.webjars:swagger-ui:5.34.1`, só arquivos estáticos (JS, CSS e PNG), sem Spring

**Storage**: N/A (nenhuma tabela, script SQL ou dado persistido muda)

**Testing**: JUnit 6.1.3 pelo Maven Wrapper (`sh ./mvnw clean verify`), com o servidor real em porta livre e repositórios em memória. Verificação da página com Chrome headless e validação do OpenAPI com ferramenta descartável (`npx @redocly/cli`), ambas fora do repositório

**Target Platform**: API local (`http://localhost:8080`) e emulador Android (`http://10.0.2.2:8080`); navegador atual no computador

**Project Type**: web-service (monólito modular, um módulo Maven) com app Android que consome a API

**Performance Goals**: sem meta nova. A página carrega cerca de 2 MB de arquivos locais; com `no-store`, eles são baixados de novo a cada visita, o que é aceitável em uso local

**Constraints**: sem CDN nem recurso externo; sem CORS; sem mudar comportamento, status, formato ou autorização de rota existente; política de conteúdo estrita mantida fora de `/docs`; versões fixas

**Scale/Scope**: 29 rotas existentes e 2 novas de documentação; 1 contrato com cerca de 1.700 linhas antes da história e cerca de 2.700 depois dos exemplos e grupos; 1 roteiro; 1 coleção Postman

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Situação | Observação |
|---|---|---|
| I. Escopo acadêmico e dados sintéticos | OK | Exemplos e roteiro usam só contas `.invalid`, amostras de `samples/` e o protocolo fictício; a página e o contrato identificam leitura, resumo e submissão como simulados |
| II. Limites clínicos | OK | Nada muda no domínio; os textos não prometem diagnóstico, urgência, vaga nem aprovação |
| III. Java puro, Jetty, monólito, camadas | OK | Um servlet sem regra de negócio, sem Repository e sem Service (conteúdo estático, como a rota coringa), e nenhum Spring. A webjar é só um pacote de arquivos estáticos |
| IV. Segurança e autorização por recurso | OK, com justificativa | Rota pública nova que só expõe o contrato, já público no repositório. A política de conteúdo de `/docs` é mais permissiva (scripts e estilos próprios, estilos inline, imagens `data:`, conexões só com a mesma origem), mas restrita a essas respostas; ver Complexity Tracking. Nenhuma outra rota muda |
| V. Regras determinísticas e IA | OK | Não muda |
| VI. Persistência e revisões | OK | Não muda |
| VII. Verificação honesta | OK | Testes automatizados do endpoint, da coerência dos documentos e do roteiro em memória; página verificada em navegador real; Oracle real só com autorização. Nenhum teste é enfraquecido |
| VIII. Código documentado e mudanças mínimas | OK | JavaDoc em português, alterações pequenas; README, docs/ e contratos atualizados na mesma entrega |
| Restrições: versões fixas, poucas bibliotecas | OK, com justificativa | Uma dependência nova, com versão fixa, exigida pelo critério "Swagger" e pela decisão do usuário |

Re-check pós-design (Phase 1): sem mudança. O contrato de [contracts/api-documentacao.md](contracts/api-documentacao.md) mantém as duas exceções justificadas abaixo, e o restante continua OK.

## Project Structure

### Documentation (this feature)

```text
specs/028-documentar-api-swagger/
├── plan.md                      # este arquivo
├── research.md                  # decisões D1–D12
├── data-model.md                # recursos servidos e passos do roteiro (sem persistência)
├── quickstart.md                # como verificar a entrega
├── contracts/
│   └── api-documentacao.md      # contrato de GET /docs e GET /docs/{arquivo}
├── checklists/requirements.md
└── tasks.md                     # gerado por /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── pom.xml                                          # + webjar swagger-ui 5.34.1; + recurso ../docs/api/openapi.yaml
├── src/main/java/br/com/regulacheck/
│   ├── documentacao/
│   │   ├── DocumentacaoController.java              # novo: GET /docs e lista fixa de arquivos
│   │   └── package-info.java                        # novo
│   ├── auth/autorizacao/RotasPrivadas.java          # PUBLICAS + "/docs/*"
│   └── config/Composicao.java                       # registra /docs/*
├── src/main/resources/documentacao/
│   ├── index.html                                   # novo: página própria
│   └── inicializar.js                               # novo: configuração do Swagger UI
└── src/test/java/br/com/regulacheck/documentacao/
    ├── DocumentacaoEndpointTest.java                # página, contrato idêntico, tipos, 404/405, CSP restrita a /docs
    ├── ContratoDaApiTest.java                       # CONTRATO_API.md × openapi.yaml (método + caminho)
    └── RoteiroAvaliacaoTest.java                    # percurso do roteiro pelas rotas reais em memória

docs/api/openapi.yaml                                # tags, exemplos, descrição, servidor relativo, 202
docs/api/roteiro-avaliacao.md                        # novo: procedimento ponta a ponta (Swagger UI e curl)
docs/api/RegulaCheck.postman_collection.json         # + admin; exclusão sobre rascunho descartável
docs/api/README.md, CONTRATO_API.md, README.md, docs/ambiente.md, docs/decisoes.md,
docs/api/protecoes.md, docs/api/autorizacao.md, docs/api/erros.md, docs/README.md
docs/evidencias/2026-11-19_rc28-documentacao-api.md  # novo: registro da verificação real
```

**Structure Decision**: o pacote `documentacao` fica na raiz de `br.com.regulacheck`, como `saude`, porque é um recurso técnico sem domínio, sem Service e sem Repository. O nome é diferente de `documento` (documentos da paciente), e o `package-info.java` explica a diferença. Os testes ficam no pacote espelho.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Política de conteúdo mais permissiva em `/docs` (`script-src 'self'`, `style-src 'self' 'unsafe-inline'`, `img-src 'self' data:`, `connect-src 'self'`) | O Swagger UI precisa executar seus scripts, aplicar estilos inline e mostrar ícones embutidos; o "Try it out" usa `fetch` na mesma origem | Manter `default-src 'none'` impede a página de funcionar. Abrir CORS para um Swagger externo afrouxaria **todas** as rotas, e o CLAUDE.md proíbe liberar origens com credenciais. A exceção fica só nas respostas de sucesso de `/docs`, sem `unsafe-eval` e sem scripts inline |
| Nova dependência `org.webjars:swagger-ui` | Critério de aceite "Swagger" e decisão do usuário de 19/11/2026 | CDN carrega código de terceiros a cada acesso e exige rede; copiar os arquivos para o repositório cria cerca de 2 MB versionados sem gestão de versão; Swagger em Docker exige CORS e uma infraestrutura opcional a mais |
