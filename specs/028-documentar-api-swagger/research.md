# Research: Documentar API e respostas HTTP (RC-28)

Decisões tomadas em 19/11/2026. Não restou nenhum "NEEDS CLARIFICATION" no contexto técnico.

## D1. Como servir o Swagger

- **Decisão**: o Swagger UI é servido pela própria API em `GET /docs`, com os arquivos estáticos da webjar `org.webjars:swagger-ui` (versão fixa) lidos do classpath.
- **Rationale**: é a decisão do usuário. O "Try it out" funciona na mesma origem, sem CORS, e o avaliador só precisa da API rodando. Nenhum framework é usado.
- **Alternatives considered**: (a) Só o arquivo, lido no Swagger Editor externo ou na IDE: sem CORS, o "Try it out" de outra origem falha, e o usuário preferiu servir a página. (b) CDN (unpkg/jsdelivr): busca código de terceiros a cada acesso e exige rede e uma política de conteúdo que confie no CDN. (c) Imagem Docker `swaggerapi/swagger-ui`: exige CORS na API e Docker, que é opcional no projeto. (d) Copiar os arquivos do Swagger UI para `src/main/resources`: cerca de 2 MB versionados, sem gestão de versão.

## D2. Versão do Swagger UI

- **Decisão**: `org.webjars:swagger-ui:5.34.1`, publicada em 30/10/2026, com propriedade `swagger-ui.version` no `pom.xml`.
- **Rationale**: é uma versão de correção, estável há três semanas. As notas das versões seguintes não trazem correção de segurança: a 5.35.0 (13/11) só acrescenta recursos de renderização de JSON Schema, e a 5.35.1 (19/11, dia da entrega) só atualiza a imagem Docker. O CLAUDE.md §4 e §8 pedem para não supor que a versão mais recente é a adequada.
- **Alternatives considered**: 5.35.1 (publicada hoje, sem histórico de uso) e 5.35.0 (versão de recursos, sem ganho para o contrato atual em OpenAPI 3.0.3).

## D3. Fonte única do contrato

- **Decisão**: o `pom.xml` passa a declarar `<resources>`: `src/main/resources` (o padrão, agora explícito) e `${project.basedir}/../docs/api`, com `include openapi.yaml` e `targetPath documentacao`. O jar leva `documentacao/openapi.yaml`, servido em `GET /docs/openapi.yaml`. Um teste compara byte a byte o que é servido com `../docs/api/openapi.yaml`.
- **Rationale**: atende o FR-002 sem cópia manual; editar o arquivo em `docs/api` muda o que é servido no próximo build.
- **Alternatives considered**: mover o contrato para `backend/src/main/resources` (quebra os links de `docs/api` e o hábito do grupo); copiar com `maven-resources-plugin` numa execução própria (mais configuração para o mesmo efeito).

## D4. Página e inicialização próprias

- **Decisão**: `index.html` e `inicializar.js` próprios, em `src/main/resources/documentacao/`. Da webjar são usados só `swagger-ui.css`, `swagger-ui-bundle.js`, `index.css` e os dois favicons. Configuração: `url: "/docs/openapi.yaml"`, `layout: "BaseLayout"` (sem a barra superior que carrega URLs arbitrárias), `validatorUrl: null` (não envia o contrato a `validator.swagger.io`), `persistAuthorization: false` (token só na memória da aba), `deepLinking: true`, `filter: true`, `displayRequestDuration: true`, `tryItOutEnabled: false`.
- **Rationale**: o `index.html` e o `swagger-initializer.js` da webjar apontam para o Petstore e usam o `StandaloneLayout`, cuja barra superior aceita outra URL de contrato. Os scripts ficam em arquivo, nunca inline, para que a política de conteúdo não precise de `'unsafe-inline'` em `script-src`.
- **Alternatives considered**: reescrever o initializer da webjar em tempo de execução (frágil); servir a página da webjar como está (aponta para fora).

## D5. Lista fixa de arquivos e falha na subida

- **Decisão**: o `DocumentacaoController` monta, no construtor, um mapa imutável `nome público → (bytes, Content-Type)` para os 8 arquivos (página, inicializador, contrato, `swagger-ui.css`, `swagger-ui-bundle.js`, `index.css` e 2 favicons). O caminho da URL só serve de chave nesse mapa e nunca compõe um caminho de classpath, o que impede travessia de pasta. A versão da webjar vem de `META-INF/maven/org.webjars/swagger-ui/pom.properties`, então a versão fica escrita num lugar só (o `pom.xml`). Se faltar algum arquivo, uma `IllegalStateException` com mensagem curta impede a subida.
- **Rationale**: um defeito de empacotamento aparece no build/teste e na subida, nunca como uma página quebrada em silêncio (mesmo padrão do `LeitorSimulado`). São cerca de 2 MB em memória, imutáveis e sem dado de paciente, então o servlet compartilhado não guarda estado por requisição.
- **Alternatives considered**: ler a cada requisição (mais E/S sem ganho); repetir a versão numa constante Java (duplica a versão do `pom.xml`); filtragem de recurso do Maven (mais configuração).

## D6. Política de conteúdo só em `/docs`

- **Decisão**: o filtro global `CabecalhosSegurosFiltro` não muda. O `DocumentacaoController`, ao servir um arquivo com sucesso, troca só o `Content-Security-Policy` por `default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`. As respostas de erro (404/405) de `/docs` continuam com a política estrita. `nosniff`, `no-store`, `no-referrer` e o HSTS (quando houver HTTPS) continuam vindo do filtro.
- **Rationale**: a exceção fica onde é necessária, e o teste existente (`CabecalhosSegurosTest`) continua exigindo a política estrita nas demais rotas. `'unsafe-inline'` vale só para estilos, porque o Swagger UI aplica atributos `style`; scripts continuam só `'self'`, sem `unsafe-eval`. A ausência de violações é conferida num navegador real (Chrome headless).
- **Alternatives considered**: regra por caminho dentro do filtro global (acopla o filtro à documentação); `style-src-attr` separado (o suporte varia e o ganho é pequeno).

## D7. Rota pública e métodos

- **Decisão**: `RotasPrivadas.PUBLICAS` recebe `"/docs/*"`, e o `Composicao` publica o servlet nesse caminho, antes da rota coringa. O mapeamento `/docs/*` atende `/docs`, `/docs/` e `/docs/{arquivo}`. Só GET é aceito (base `ServletJson`, então os outros métodos recebem 405 `METODO_NAO_PERMITIDO` com `Allow: GET`). Caminhos fora da lista recebem 404 `NAO_ENCONTRADO` idêntico ao da rota coringa. A página usa URLs absolutas (`/docs/...`), então `/docs` funciona sem redirecionamento.
- **Rationale**: o registro de rotas recusa a subida com caminho publicado fora da lista (`exigirTudoDeclarado`); a rota é pública por desenho, como `/health`.

## D8. Cache

- **Decisão**: manter `Cache-Control: no-store` também em `/docs`.
- **Rationale**: não afrouxar mais um cabeçalho de segurança. Em uso local, baixar de novo cerca de 2 MB por visita é aceitável.

## D9. Exemplos do contrato

- **Decisão**: os exemplos de resposta vêm de uma execução real do roteiro pelas rotas da API com repositórios em memória (os mesmos usados no `RoteiroAvaliacaoTest`), registrada temporariamente fora do repositório e transcrita para `examples` no contrato. Os exemplos de entrada vêm dos corpos usados no roteiro. A senha nunca entra como valor: aparece um marcador `<senha de demonstração — database/scripts/README.md>`. O token de exemplo é um texto fictício, sem o prefixo `Bearer`. A conformidade dos exemplos com os esquemas é validada pela regra `no-invalid-media-type-examples` do Redocly.
- **Rationale**: atende FR-009/FR-010 sem inventar campos, e a varredura de segredos (`VarreduraDeSegredosTest`) aceita marcadores com `<`.

## D10. 202 e falhas de dependência

- **Decisão**: a descrição geral do contrato declara que **nenhuma rota usa 202**, porque leitura, análise, resumo, reenvio e submissão são síncronos e só respondem depois de gravar. As respostas 503 são documentadas por rota com o código estável (`DEPENDENCIA_INDISPONIVEL`, `LEITURA_INDISPONIVEL`, `ANALISE_INDISPONIVEL`) e exemplos. A queda do resumo simulado aparece como exemplo 201 com `origem: PADRAO`.
- **Rationale**: o critério pede "202 quando usado"; documentar um 202 inexistente seria falso.

## D11. Roteiro e verificação automática

- **Decisão**: `docs/api/roteiro-avaliacao.md` traz os passos com Swagger UI e com `curl`, usando `samples/encaminhamento-sintetico.pdf` (primeiro envio; falta a identificação, então RC-R03 fica pendente) e `samples/identificacao-sintetica.png` (correção). Depois vêm o reenvio, que para na conferência, a confirmação, o reenvio de novo (`SEM_PENDENCIAS_IDENTIFICADAS`) e a submissão simulada. O DELETE é feito sobre um rascunho descartável, e o passo administrativo usa `admin@exemplo.invalid`. O `RoteiroAvaliacaoTest` executa a mesma sequência pelas rotas reais com repositórios em memória, o leitor simulado de produção e os arquivos de `samples/`, conferindo status e campos de cada passo.
- **Rationale**: atende FR-011 a FR-013. Os arquivos da raiz de `samples/` são os mesmos do conjunto 01, já reconhecidos pelo leitor simulado.

## D12. Coerência dos documentos e da coleção Postman

- **Decisão**: o `ContratoDaApiTest` extrai os pares método + caminho da tabela de `CONTRATO_API.md` e do bloco `paths` do `openapi.yaml` (leitura por indentação: caminhos com 2 espaços e métodos com 4, como no arquivo escrito à mão) e exige igualdade e uma `tag` em cada operação. Na coleção Postman entram o login do administrador e `GET /admin/indicadores`, e a exclusão do rascunho passa a usar um rascunho descartável criado para isso (variável própria), no fim da coleção.
- **Rationale**: o teste não exige nova biblioteca de YAML, e o formato do arquivo é controlado pelo grupo. A coleção deixa de apagar o pedido do percurso.
