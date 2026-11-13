# Arquitetura

Monolito modular em Java puro: um processo, um módulo Maven (`backend/`), pacotes por funcionalidade em `br.com.regulacheck`, cada um com as camadas controller/service/repository/entity/dto conforme necessário. Cada pacote ainda vazio tem um `package-info.java` descrevendo sua finalidade; não há classes de negócio. Exemplo mínimo das camadas: pacote `saude` (`GET /health`).

## Tecnologia (exigência da disciplina, decisão de 12/11/2026)
- **Sem Spring e sem JPA/Hibernate ORM no código de produção.** Exceção: Spring Boot pode ser usado apenas em testes automatizados (escopo `test` no Maven). A camada HTTP usa Jakarta Servlet em servidor embutido (Jetty ou Tomcat); os controllers são servlets.
- **Banco Oracle fornecido pela faculdade**, acessado por JDBC com SQL parametrizado. Os repositories executam o SQL diretamente; o esquema vem dos scripts em `database/scripts/` (modelo e dicionário em [modelo-de-dados.md](modelo-de-dados.md); tabelas com prefixo `RC_`).
- Sem container de injeção de dependência: as dependências são montadas manualmente por construtor em uma classe de composição (`config`).
- **Telemetria IoT:** o script em `simulador-iot/` simula os dois sensores e envia leituras em JSON por HTTP POST; o módulo `iot` do backend valida (credencial, sessão de captura, faixa, atualidade) e persiste. Sem broker de mensagens.
- **Migração concluída para a base técnica** (RC-02): Jetty 12.1.15 e Servlet em Java puro, sem Spring/JPA; o esquema Oracle e os scripts SQL foram entregues na RC-03; **o acesso a dados por JDBC começou na RC-04** (pool Hikari de no máximo 3 conexões, criado só no primeiro uso, então a API sobe sem banco e as rotas que o usam respondem 503). Ver [decisoes.md](decisoes.md).
- **Autenticação (RC-04):** `POST /auth/login` confere a senha com BCrypt e abre uma sessão; o token (256 bits) vai à cliente e só o SHA-256 dele fica em `RC_SESSAO_LOGIN`. As rotas protegidas passam por `AutenticacaoFiltro` (`auth/controller`), que valida `Authorization: Bearer`, grava a identidade na requisição e responde 401 sem sessão válida. Para proteger uma rota nova, registre o filtro no caminho dela em `config/Composicao`. Perfis e isolamento por paciente são da RC-05. Os testes injetam repositórios em memória por `Servidor.criar(Configuracao, Dependencias)`.
- **Autorização (RC-05):** toda rota privada é publicada por `RotasPrivadas` (`auth/autorizacao`), que instala `AutenticacaoFiltro` (401) e `AutorizacaoFiltro` (403 por perfil); a dona do recurso é conferida no Service com `Propriedade` sobre uma busca já filtrada pela dona (404 idêntico para alheio e inexistente). A chave do simulador (`IOT_API_KEY`) é verificada por `SimuladorFiltro` apenas na telemetria. `Composicao.montar` falha se algum servlet for publicado fora do registro. Ordem: 401 → 403 → 404 → 400; guia em [api/autorizacao.md](api/autorizacao.md).

- **Proteções de entrada e processamento (RC-06):** o arquivo recebido passa por `ConferenciaDeArquivo` (`armazenamento/conferencia`: tipo real pelos primeiros bytes, extensão, 5 MB com leitura que para no limite, páginas e proteção de PDF pelo PDFBox somente leitura, cabeçalho e dimensões de imagem) e só então é guardado por `ArmazenamentoPrivado` (`armazenamento`; nome interno aleatório, gravação atômica, sem travessia de diretório). O download usa `DownloadSeguro` (`common/http`) atrás de `RotasPrivadas` + `Propriedade`. `CabecalhosSegurosFiltro` e o escape de `<`, `>`, `&`, `'` no `Json` mantêm o texto inerte; `RedatorDeLog` (`common/log`) mascara segredos nas falhas registradas; `TextoNaoConfiavel` e `ValidadorDeSaidaEstruturada` (`integracao`) tratam o conteúdo de documentos como dado e rejeitam saída fora do contrato; `ConfiguracaoDeCanal` (`config`) recusa `API_PUBLIC_URL` remota sem https. Guia: [api/protecoes.md](api/protecoes.md).
- **Rascunho e serviço (RC-07):** `GET /protocolos` e `GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}` (só `PACIENTE`, registradas por `RotasPrivadas`). `ProtocoloController`/`SolicitacaoController` só traduzem HTTP; `ProtocoloService`/`SolicitacaoService` aplicam o caso de uso (a paciente vem da sessão e o pedido é lido por `Propriedade` sobre `buscarDaPaciente`, já filtrado pela dona); `ProtocoloRepositoryJdbc`/`SolicitacaoRepositoryJdbc` fazem o SQL parametrizado. Criar grava `RC_SOLICITACAO` e a revisão 1 numa transação; trocar o serviço bloqueia a linha (`SELECT … FOR UPDATE`), confere o estado e, se o serviço mudou, incrementa a revisão e grava `RC_REVISAO` com `PROTOCOLO_ALTERADO`. A atomicidade fica no Repository (o `COMMIT` só depois de todos os passos); o Service traduz o resultado em 404/409/422. Plano e decisões: [specs/008-rascunho-selecionar-servico/](../specs/008-rascunho-selecionar-servico/plan.md).

## Módulos
config, auth, usuario, solicitacao, documento, extracao, protocolo, analise, validacao (model, regra), integracao/ia, integracao/extracao, armazenamento (+ conferencia), iot, submissao, indicador, integracao (conteúdo não confiável), common/exception, common/http, common/log.

## Regras de dependência
- Controller -> Service (nunca Repository). Service -> Repository, motor de regras, contratos de integração.
- Repository não chama Controller, Service nem IA.
- Regras documentais não dependem de HTTP, JDBC ou SDK de IA.
- Adaptadores externos não decidem o estado de negócio.
- IA explica resultados; não substitui o motor determinístico.
- Entidades não são expostas na API (usar DTOs). Nenhuma credencial vai ao mobile.

## Estados da solicitação (propostos)
RASCUNHO, PROCESSANDO, REQUER_CONFERENCIA, COM_PENDENCIAS, SEM_PENDENCIAS_IDENTIFICADAS, FALHA_ANALISE, SUBMETIDA_SIMULACAO.

Transições, revisões e regras completas em [api/estados-e-revisoes.md](api/estados-e-revisoes.md). Resumo: RASCUNHO -> PROCESSANDO -> (REQUER_CONFERENCIA | COM_PENDENCIAS | SEM_PENDENCIAS_IDENTIFICADAS | FALHA_ANALISE); após correção ou reenvio vai direto a PROCESSANDO com revisão + 1 (nunca volta a RASCUNHO depois do primeiro processamento); alteração durante PROCESSANDO inicia nova revisão e o resultado da antiga não é publicado; somente SEM_PENDENCIAS_IDENTIFICADAS -> SUBMETIDA_SIMULACAO. `REQUER_CONFERENCIA` cobre também o caso de haver regra não verificável (ver [protocolo-academico.md](protocolo-academico.md#resultado-geral)). O estado do resumo de IA é distinto do estado da validação. Implementação futura.

## Contratos
Ver [api/README.md](api/README.md) e [../CONTRATO_API.md](../CONTRATO_API.md). A conferência da transcrição é separada de alteração clínica do documento original; informação clínica ausente é corrigida pela unidade emissora. O sistema não diagnostica, não define urgência nem autoriza atendimento; a submissão é simulada.
