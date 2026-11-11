# RegulaCheck — Estrutura inicial Java e preparação do ambiente

Documento para fornecer ao Claude como instrução de implementação da fundação do projeto. Data: 11/11/2026. Prazo do MVP completo: 19/11/2026. Este documento especifica apenas a preparação inicial; não representa código já criado nem ambiente já verificado.

## 1. Instrução principal para o Claude

Prepare a estrutura inicial do RegulaCheck usando Java e Spring Boot, com arquitetura de monólito modular e camadas Controller, Service e Repository dentro de cada funcionalidade. Entregue uma aplicação base compilável, configurações locais documentadas, estrutura de pastas e um roteiro reproduzível de execução.

Nesta etapa, escreva somente o código técnico indispensável para inicializar, compilar e verificar a aplicação. Não implemente os casos de uso do MVP, regras clínicas/documentais, entidades de negócio, CRUDs, integração de IA, autenticação completa ou simulação funcional de sensores. Esses recursos serão implementados depois, com base nas histórias do backlog.

Antes de alterar arquivos, inspecione o repositório existente e respeite suas instruções. Preserve o trabalho já realizado. Não sobrescreva, apague ou reinicialize um projeto existente sem necessidade. Não crie commits, publique código ou faça deploy sem solicitação específica. Se houver uma configuração Java válida, priorize aproveitá-la.

## 2. Contexto para orientar a organização

A usuária principal é a paciente. Ela envia encaminhamento e documentos; o sistema extrai informações, permite conferir a transcrição, compara dados com critérios versionados e gera um resumo de pendências por IA. A paciente providencia correções e reenvia documentos. Informações clínicas ausentes ou incorretas no documento devem ser corrigidas pela unidade emissora, não inventadas pela aplicação.

O MVP usa somente dados sintéticos e um protocolo acadêmico fictício. A submissão ao órgão do SUS será simulada, sem colocar ninguém em fila real. O sistema não diagnostica, não define urgência e não autoriza atendimento.

A faculdade exige API Java com Controller, Service e Repository, banco relacional, app mobile consumindo a própria API, segurança, testes, documentação e pelo menos dois sensores simulados. Tecnologias fora do conteúdo até a fase 7 podem não pontuar.

## 3. Decisões e informações ainda não confirmadas

| Item | Diretriz para esta etapa |
|---|---|
| Linguagem do backend | Java, obrigatória; não usar C# |
| Framework proposto | Spring Boot, sujeito à compatibilidade com o conteúdo da disciplina |
| Organização | Uma aplicação backend, um módulo Maven e pacotes por funcionalidade |
| Build | Maven Wrapper, incluindo scripts Unix e Windows |
| JDK | Usar a versão exigida pela disciplina/projeto existente; em projeto novo sem indicação, propor Java 21 e registrar a escolha |
| Versão do Spring Boot | Fixar versão estável compatível com o JDK escolhido; conferir documentação oficial no momento da criação; não usar SNAPSHOT nem versão dinâmica |
| Banco | Reutilizar o banco definido pela disciplina. Se não houver indicação, registrar PostgreSQL como proposta, ainda não como requisito confirmado |
| Mobile | Tecnologia ainda não informada; não escolher React Native, Flutter ou Android nativo por conta própria |
| IA e extração | Provedores ainda não definidos; preparar documentação e limites de integração, sem SDK obrigatório nesta etapa |
| Docker | Opcional para desenvolvimento local, se disponível e permitido; execução Java deve ser documentada independentemente dele |
| Controle de versão | Usar repositório existente ou preparar arquivos locais; sem publicação automática |

Não bloqueie a estrutura do backend por informações ainda ausentes. Prepare a fundação independente de banco, mobile e provedor de IA e liste o que precisa de decisão. Não declare o ambiente inteiro pronto se apenas o backend foi verificado.

## 4. Arquitetura e limites de dependência

A API é um monólito modular: um processo e uma unidade de execução, com módulos lógicos internos. Não criar microsserviços, API Gateway, Kafka, RabbitMQ, Kubernetes, servidor de descoberta ou vários projetos Maven para este MVP.

Fluxo de responsabilidades previsto:

1. O mobile e o simulador se comunicam por HTTP com a API.
2. Controllers recebem requisições, validam contratos e traduzem resultados para HTTP.
3. Services coordenam casos de uso, autorização e regras de estado/revisão.
4. O motor de validação executa as regras aplicáveis e agrega resultados.
5. Repositories cuidam da persistência.
6. Adaptadores isolam extração, IA e armazenamento privado.

Regras de dependência:

- Controller chama Service; não acessa Repository diretamente.
- Service pode usar Repository, motor de regras e contratos de integração.
- Repository não chama Controller, Service ou provedor de IA.
- Regras documentais não dependem de HTTP, banco ou SDK de IA.
- Adaptadores externos não decidem o estado de negócio da solicitação.
- A implementação futura de IA explica resultados, mas não substitui o motor determinístico.
- Entidades persistidas não são expostas diretamente na API; usar DTOs.
- Nenhuma credencial de IA ou de banco é enviada ao mobile.

Esses limites são convenções documentadas; não é necessário adicionar framework para fiscalizar dependências nesta etapa.

## 5. Padrões previstos e uso proporcional

| Padrão ou técnica | Aplicação futura | Preparar agora |
|---|---|---|
| Service Layer | Coordenação dos casos de uso | Pacotes e convenções de responsabilidade |
| Repository | Acesso a dados | Pacotes; ativar persistência apenas quando configurada |
| Strategy | Tipos de regra documental | Pacote de contratos e implementações; sem regras de negócio prontas |
| Adapter | OCR/extração, IA e armazenamento | Pacotes e documentação dos limites |
| Injeção pelo construtor | Composição de componentes | Usar no código técnico em que houver dependências |
| DTO | Contratos HTTP próprios | Pacotes e convenção de nomes |
| Estados explícitos | Transições da solicitação | Documentar os estados; implementação virá depois |

Service Layer, Repository e DTO são padrões de aplicação; injeção de dependência é uma técnica. Não é necessário apresentar tudo como padrão GoF.

Não criar uma interface para cada classe sem motivo. Não introduzir GenericRepository próprio, AbstractBaseService, Service Locator, singleton manual ou fábrica genérica. Spring Data pode fornecer os repositórios futuros. Não adicionar classes vazias só para simular implementação.

## 6. Estrutura do repositório

Os caminhos abaixo são relativos à raiz do repositório. Criar as pastas com um README de propósito quando ainda não houver arquivos reais. Para pacotes Java ainda vazios, preferir package-info.java com documentação curta; não criar classes de negócio fictícias. Cada arquivo deve ter uma finalidade explícita.

| Caminho | Conteúdo inicial |
|---|---|
| README.md | Visão geral, pré-requisitos, execução e estado real da preparação |
| .gitignore | Exclusão de segredos, builds, arquivos locais e uploads |
| .gitattributes | Padronização de finais de linha, especialmente Maven Wrapper |
| .env.example | Nomes de variáveis, valores fictícios e orientação de configuração |
| backend/ | Aplicação Java Spring Boot |
| mobile/README.md | Tecnologia pendente, telas previstas e contrato com a API |
| simulador-iot/README.md | Contrato futuro dos sensores, sem implementação funcional agora |
| database/README.md | Banco pendente/confirmado e instruções futuras |
| database/scripts/README.md | Ordem futura: criação, carga inicial e consultas |
| docs/arquitetura.md | Decisões, módulos, responsabilidades e limites |
| docs/ambiente.md | Instalação, comandos e diagnóstico de problemas |
| docs/decisoes.md | Decisões adotadas, propostas e pendências |
| docs/testes/README.md | Estratégia e local das evidências |
| docs/evidencias/README.md | Convenção para futuras evidências reais |
| docs/api/README.md | Contratos futuros e formato de documentação escolhido |
| samples/README.md | Somente documentos sintéticos; sem dados reais |
| entrega/README.md | Checklist futuro de ZIP, PDF, SQL, TXT e pitch |

Não criar PDF, vídeo, evidências fictícias nem um ZIP final nesta preparação. Não modificar um AGENTS.md existente; em repositório novo, um novo AGENTS.md é opcional e deve apenas registrar convenções compatíveis com este documento.

## 7. Estrutura do backend

| Caminho dentro de backend/ | Finalidade |
|---|---|
| pom.xml | Build e dependências necessárias à base |
| mvnw | Execução Maven no Unix |
| mvnw.cmd | Execução Maven no Windows |
| .mvn/wrapper/ | Arquivos oficiais do Maven Wrapper |
| src/main/java/br/com/regulacheck/RegulaCheckApplication.java | Classe principal com Spring Boot |
| src/main/resources/application.yml | Configuração comum, sem segredos |
| src/main/resources/application-local.yml | Configuração de desenvolvimento local |
| src/test/java/br/com/regulacheck/ | Verificações técnicas mínimas |
| src/test/resources/ | Configuração de testes, se necessária |

Colocar RegulaCheckApplication no pacote raiz br.com.regulacheck. Evitar default package e garantir correspondência entre diretório e declaração package.

### Pacotes por funcionalidade

Abaixo, todos os caminhos são relativos a src/main/java/br/com/regulacheck/.

| Pacote | Subpacotes a preparar | Responsabilidade futura |
|---|---|---|
| config | Sem subdivisão obrigatória | Configuração transversal |
| auth | controller, service, dto | Login e sessão |
| usuario | entity, repository | Conta e perfil |
| solicitacao | controller, service, repository, entity, dto | Rascunho, edição, listagem e revisão |
| documento | controller, service, repository, entity, dto | Upload, consulta e substituição |
| extracao | controller, service, dto | Extração e conferência da transcrição |
| protocolo | controller, service, repository, entity, dto | Critérios e suas versões |
| analise | controller, service, repository, entity, dto | Execuções e histórico |
| validacao | model, regra | Contexto, resultados e motor de regras |
| integracao/ia | Sem subdivisão obrigatória | Contrato e adaptador de resumo por IA |
| integracao/extracao | Sem subdivisão obrigatória | Implementação de leitura de PDF/imagem |
| armazenamento | Sem subdivisão obrigatória | Contrato e implementação privada de arquivos |
| iot | controller, service, repository, entity, dto | Sessão de captura e leituras |
| submissao | controller, service, repository, entity, dto | Envio simulado e referência interna |
| indicador | controller, service, repository, dto | Agregados administrativos |
| common/exception | Sem subdivisão obrigatória | Erros e tratamento global |

Pacotes transversais devem permanecer pequenos. Não criar uma pasta utils para concentrar regras de negócio.

### Classes previstas, não implementar agora

Estes nomes orientam o trabalho posterior; não são uma lista de arquivos vazios obrigatórios:

- SolicitacaoController, SolicitacaoService, SolicitacaoRepository e Solicitacao.
- DocumentoController, DocumentoService, DocumentoRepository e Documento.
- ExtracaoService e ConferenciaExtracaoRequest.
- AnaliseController, AnaliseService, AnaliseRepository e Analise.
- MotorValidacao, RegraValidacao, ContextoValidacao e ResultadoRegra.
- ExtratorDocumento e GeradorResumo, com adaptadores reais a definir.
- ArmazenamentoDocumento e implementação local privada.
- LeituraSensor, SessaoCaptura e respectivos serviços/repositórios.
- SubmissaoSimuladaService e IndicadorService.

Não gerar métodos com retornos fixos, TODOs que pareçam endpoints completos ou serviços que indiquem sucesso sem executar operações reais.

## 8. Dependências e inicialização incremental

### Adicionar à base executável

- Spring Boot Web para HTTP.
- Spring Boot Validation para validação futura de contratos.
- Spring Boot Test para as verificações técnicas mínimas.
- Health técnico via Actuator, se permitido no conteúdo, ou endpoint técnico mínimo equivalente.

Fixar versões reais e compatíveis no pom.xml e Maven Wrapper. Usar o gerenciamento de dependências do Spring Boot; não misturar versões arbitrárias dos componentes Spring.

### Preparar decisão sem quebrar o boot

- Spring Data JPA e driver: ativar depois de confirmar e configurar o banco. Não adicionar dependência de datasource sem configuração e entregar aplicação que não inicia.
- Spring Security: pode ser adicionado agora somente com configuração explícita do acesso técnico; não entregar senha automática inesperada nem afirmar que a autenticação do MVP está implementada. Se adiado, documentar claramente que não existem endpoints de negócio e que a base é local.
- Documentação API: Postman Collection mínima do health é suficiente nesta etapa; Swagger pode ser preparado se a biblioteca estiver autorizada e compatível. A documentação completa virá com os endpoints.
- IA, OCR, mensageria e bibliotecas extras: não instalar SDKs ainda sem provedor e necessidade definidos.
- Evitar Lombok e mapeadores automáticos nesta etapa, salvo padrão existente do projeto.

Escolher o mínimo necessário para uma base que compila e inicia. Não substituir o banco final por H2 silenciosamente. Se um perfil de teste usar banco diferente no futuro, documentar a diferença e suas limitações.

## 9. Configuração do ambiente

### Verificações iniciais

Inspecionar e registrar, sem imprimir credenciais:

- Sistema operacional e shell disponíveis.
- java -version e disponibilidade de JDK compatível.
- Existência de Maven Wrapper e acesso às dependências.
- Porta HTTP proposta, inicialmente 8080, e eventuais conflitos.
- Banco local existente e eventual disponibilidade de Docker, sem tratá-lo como obrigatório.
- Tecnologia mobile já presente no repositório, caso exista.

Não executar instalação global, alterar configuração permanente da máquina ou remover aplicações existentes como efeito colateral. Quando faltar ferramenta, registrar instrução de instalação compatível com o sistema e continuar as tarefas independentes.

### Variáveis propostas

| Variável | Uso | Situação nesta etapa |
|---|---|---|
| SERVER_PORT | Porta do backend | Ativa, com padrão 8080 |
| DB_URL | Conexão JDBC | Reservada até configurar banco |
| DB_USERNAME | Usuário do banco | Reservada; nunca fixar segredo no código |
| DB_PASSWORD | Senha do banco | Reservada; valor real fora do Git |
| DOCUMENT_STORAGE_PATH | Diretório privado de arquivos | Reservada para upload futuro |
| AI_BASE_URL | Endereço do provedor | Reservada |
| AI_API_KEY | Credencial de IA | Reservada, sem valor real no exemplo |
| AI_MODEL | Modelo escolhido | Reservada |
| IOT_API_KEY | Credencial restrita do simulador | Reservada |

Variáveis reservadas não devem ser obrigatórias para subir a base. Não inventar endereço de provedor nem credenciais de produção.

O Spring Boot não carrega um arquivo .env automaticamente apenas porque ele existe. Documentar como exportar as variáveis no shell ou configurar a IDE. Se optar por outro carregamento explícito, explicar o mecanismo e a dependência usada. Nunca afirmar que copiar .env.example basta sem implementar esse carregamento.

### Portabilidade

Documentar comandos para Windows PowerShell e para Linux/macOS, com atenção a:

- mvnw.cmd no Windows e ./mvnw no Unix.
- Caminhos de arquivos independentes do sistema operacional.
- Execução dos comandos a partir da pasta backend.
- No Android Emulator padrão, o endereço do host pode ser 10.0.2.2; isso depende do ambiente. Dispositivo físico usa endereço acessível na mesma rede ou configuração equivalente. Não fixar localhost no mobile como solução universal.
- Não desativar segurança de transporte globalmente para resolver conectividade; documentar configuração de desenvolvimento restrita ao ambiente, se necessária.

Se Docker for usado para o banco confirmado, fixar imagem com versão explícita, volume local, porta e credenciais de desenvolvimento externas. Documentar início, parada e preservação dos dados; não executar limpeza destrutiva de volumes.

## 10. Contratos e estados a documentar

Registrar estes contratos em docs/arquitetura.md ou docs/api/README.md, sem criar endpoints de negócio funcionais agora:

- Autenticação e isolamento de dados por dona da solicitação.
- Solicitação vinculada à paciente autenticada, protocolo e revisão documental.
- Upload com limites previstos: 3 arquivos, até 5 MB cada, PDF até 5 páginas; JPEG/PNG; somente material sintético.
- Extração com origem e distinção entre identificado, não localizado e ilegível.
- Conferência da transcrição separada de alteração clínica do documento original.
- Resultado por regra: atende, pendente, não verificável ou não aplicável.
- Análise vinculada à revisão e à versão do protocolo; resultado antigo não libera revisão nova.
- Resumo por IA validado, sem inventar requisitos; falha do resumo não apaga a validação.
- Submissão idempotente, interna e explicitamente simulada.
- Telemetria com sessão vinculada ao pedido, tipo, valor, unidade e data.

Estados propostos: RASCUNHO, PROCESSANDO, REQUER_CONFERENCIA, COM_PENDENCIAS, SEM_PENDENCIAS_IDENTIFICADAS, FALHA_ANALISE e SUBMETIDA_SIMULACAO. Documentar transições previstas e distinguir o estado do resumo de IA do estado da validação; não implementar State Pattern completo nesta preparação.

## 11. Limites de segurança já na fundação

- Nenhum segredo no Git, README, configuração de exemplo ou logs.
- Diretório futuro de uploads fora da área pública/static e ignorado no Git.
- Aplicação inicial sem exposição de endpoints de negócio desprotegidos.
- Health retorna apenas estado técnico, sem variáveis de ambiente, credenciais ou detalhes internos sensíveis.
- Não expor todos os endpoints do Actuator por conveniência.
- Não configurar CORS com qualquer origem e credenciais simultaneamente. CORS é relevante para clientes de navegador; não substitui autenticação.
- Sem dados reais e sem chamadas externas automáticas de IA durante o boot ou testes de inicialização.
- A autenticação futura deverá verificar senha com mecanismo apropriado do Spring Security e aplicar permissões no servidor.

## 12. Comandos de verificação esperados

Documentar e executar quando o ambiente permitir. Registrar resultado real, sem dizer que passou se não foi executado.

Linux/macOS, a partir de backend:

```bash
java -version
./mvnw -version
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell, a partir de backend:

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Health proposto com Actuator:

```text
GET http://localhost:8080/actuator/health
Esperado: HTTP 200 e status UP.
```

Se usar endpoint técnico equivalente, registrar sua rota real. O health da fundação não comprova que banco, IA, mobile ou sensores estão integrados. Não apresentar essa resposta como validação do MVP completo.

Adicionar apenas verificações técnicas úteis: contexto inicia e rota técnica responde conforme configurado. Se Test/Security/JPA forem ativados, os testes precisam refletir a configuração real. Não gerar testes de regras de negócio ainda inexistentes.

## 13. Critérios de aceite desta etapa

- [ ] Repositório existente inspecionado e alterações anteriores preservadas.
- [ ] Backend Java com Spring Boot, sem dependência de C#.
- [ ] JDK, Spring Boot e Maven Wrapper compatíveis, com versões exatas registradas.
- [ ] Estrutura por funcionalidade com camadas Controller, Service e Repository preparada.
- [ ] Pacotes futuros documentados sem classes que simulem funcionalidades prontas.
- [ ] Backend compila e inicia com configuração local mínima.
- [ ] Health técnico funciona e não expõe detalhes sensíveis.
- [ ] Nenhuma credencial de IA ou banco necessária para inicialização antes de sua integração.
- [ ] .gitignore, .gitattributes e configurações de exemplo revisados.
- [ ] Banco, mobile e provedores confirmados ou explicitamente listados como pendências.
- [ ] README com execução no Windows e Unix, endereçamento e solução de problemas comuns.
- [ ] Pelo menos uma verificação real de build/inicialização documentada, ou bloqueio concreto registrado.
- [ ] Nenhum CRUD, motor documental, autenticação de negócio, IA ou sensor funcional implementado nesta fase.
- [ ] Nenhum deploy, publicação de repositório ou envio de documentos a terceiros realizado.

## 14. Relatório que o Claude deve entregar ao final

Responder com:

1. Estrutura efetivamente criada e arquivos técnicos adicionados/alterados.
2. Versões escolhidas e motivo, incluindo compatibilidade com o ambiente existente.
3. Comandos executados e resultados reais de build, inicialização e health.
4. Como executar localmente no sistema do usuário.
5. Decisões pendentes: banco, tecnologia mobile, regra da disciplina e provedores de IA/extração.
6. Limitações e ferramentas ausentes, sem afirmar que componentes não testados estão prontos.
7. Próxima etapa recomendada: confirmar banco e contratos; depois implementar persistência, segurança e o primeiro fluxo vertical da paciente conforme o backlog.

Não avançar automaticamente para implementação funcional após concluir esta fundação. O objetivo desta solicitação é deixar a base organizada e verificável para começarmos as histórias depois.

## Referências de arquitetura

As referências sustentam a organização proposta; não obrigam o grupo a adotar versões ou recursos fora do conteúdo da disciplina.

- Estruturação de código Spring Boot: https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/using/structuring-your-code.adoc
- Injeção de dependência Spring Framework: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
- Beans e injeção no Spring Boot: https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html
