# Feature Specification: Login e encerramento de sessão (RC-04)

**Feature Branch**: `004-login-sessao`

**Created**: 2026-11-12

**Status**: Implementado e verificado em 12/11/2026 (ver tasks.md; itens não verificados listados no relatório)

**Backlog**: RC-04 — Code-Masters-F/ClickBus#10 · Épico 2 (Conta e proteção de acesso) · P0 · até 13/11/2026 · depende da RC-02 e da RC-03 (concluídas)

**Input**: User description: "RC-04 — Fazer login e encerrar sessão. Como paciente, quero entrar com minha conta para acessar meus documentos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar com a conta de demonstração (Priority: P1)

A paciente informa e-mail e senha da sua conta e recebe uma sessão para usar a API. Se a senha estiver errada, a resposta é um erro genérico e nenhuma sessão é aberta.

**Why this priority**: sem login não há como identificar a paciente, e todas as histórias seguintes (rascunho, upload, análise) dependem da sessão.

**Independent Test**: com as contas da carga da RC-03, entrar com a conta correta e ver uma sessão válida; entrar com senha errada e ver o erro genérico sem sessão.

**Acceptance Scenarios**:

1. **Given** a conta de paciente A da carga sintética, **When** ela envia e-mail e senha corretos, **Then** recebe uma sessão com prazo de validade e seus dados básicos (nome e perfil), sem nenhum dado de senha.
2. **Given** as contas de paciente B e de administrador, **When** cada uma envia suas credenciais corretas, **Then** cada uma recebe uma sessão própria.
3. **Given** uma conta existente, **When** a senha enviada está errada, **Then** a resposta é um erro genérico de credenciais inválidas e nenhuma sessão é aberta.
4. **Given** um e-mail que não existe, **When** é enviado com qualquer senha, **Then** a resposta é idêntica à de senha errada (mesmo código, mesma mensagem, sem indicar qual dado falhou).
5. **Given** uma conta desativada, **When** as credenciais corretas são enviadas, **Then** a resposta é a mesma do erro genérico e nenhuma sessão é aberta.
6. **Given** um pedido sem e-mail ou sem senha, ou com corpo ilegível, **When** enviado, **Then** a resposta é um erro de requisição inválida com os campos faltantes, sem abrir sessão.

---

### User Story 2 - Senha protegida e verificada com segurança (Priority: P1)

A senha nunca é guardada nem comparada em texto puro. A verificação usa o mesmo algoritmo e formato de hash dos dados já carregados no banco pela RC-03, de modo que as contas de demonstração autentiquem sem recriar dados.

**Why this priority**: é exigência de segurança do projeto e requisito explícito do aceite; um erro aqui compromete todas as contas.

**Independent Test**: confirmar que a senha de demonstração documentada autentica contra os três hashes carregados, que uma senha errada não autentica, e que senha, hash e sessão não aparecem em respostas nem em logs.

**Acceptance Scenarios**:

1. **Given** os hashes carregados pela RC-03, **When** a senha de demonstração é verificada, **Then** a verificação confirma as três contas sem alterar os dados no banco.
2. **Given** uma tentativa de login, **When** ela falha ou tem sucesso, **Then** nenhum log nem resposta contém a senha, o hash ou o token da sessão.
3. **Given** a verificação de senha, **When** examinada, **Then** usa algoritmo próprio para senhas de biblioteca consolidada, e não um hash rápido caseiro.

---

### User Story 3 - Usar a sessão e encerrá-la (Priority: P1)

Enquanto a sessão é válida, a paciente é reconhecida pela sessão (nunca por um identificador enviado no corpo). Ao sair, a sessão deixa de valer imediatamente. Uma sessão expirada exige novo login.

**Why this priority**: encerrar a sessão e expirar por prazo evitam que um aparelho esquecido ou um token vazado dê acesso indefinido.

**Independent Test**: entrar, consultar a sessão atual com sucesso, sair, tentar usar o mesmo token e receber "não autenticado"; repetir deixando o prazo vencer.

**Acceptance Scenarios**:

1. **Given** uma sessão válida, **When** a paciente consulta a sessão atual, **Then** recebe a própria identidade (nome e perfil) e o instante de expiração.
2. **Given** uma sessão válida, **When** a paciente pede para sair, **Then** a sessão é encerrada e o mesmo token passa a ser recusado imediatamente.
3. **Given** uma sessão cujo prazo venceu, **When** o token é usado em qualquer rota protegida, **Then** a resposta é "não autenticado" (401) e o cliente deve pedir novo login.
4. **Given** ausência de token, token malformado ou desconhecido, **When** usado em rota protegida, **Then** a resposta é "não autenticado" (401) no formato padrão de erro, sem revelar o motivo exato.
5. **Given** duas sessões da mesma conta abertas em aparelhos diferentes, **When** uma delas sai, **Then** a outra continua válida até o seu próprio prazo.

---

### User Story 4 - Comportamento do cliente durante o login (Priority: P2)

O contrato da API e a documentação descrevem como o aplicativo deve se comportar: enquanto o login está em andamento não permite novo envio (evita cliques repetidos), mostra erro compreensível em credencial inválida e volta para o login quando a sessão expira. A tela em si será construída na história que criar o aplicativo.

**Why this priority**: o critério de aceite menciona a tela, mas o projeto Android ainda não existe; registrar o comportamento esperado garante que a futura tela o cumpra.

**Independent Test**: ler o contrato e a documentação do app e conferir que descrevem os quatro comportamentos (envio em andamento, sem repetição, erro genérico, sessão expirada pede novo login) e que a API tolera o mesmo login enviado duas vezes seguidas sem efeito inesperado.

**Acceptance Scenarios**:

1. **Given** a documentação do app, **When** consultada, **Then** descreve bloqueio de novo envio durante o login em andamento, mensagem de erro para credenciais inválidas e retorno ao login quando receber 401.
2. **Given** o mesmo login enviado duas vezes em sequência, **When** a API responde, **Then** cada envio é tratado de forma independente e as duas sessões resultantes são válidas e encerráveis, sem corromper dados.

---

### Edge Cases

- E-mail com maiúsculas, minúsculas ou espaços nas pontas é tratado como o mesmo e-mail cadastrado.
- Senha vazia, muito longa ou com caracteres especiais e acentos é tratada sem falha interna e sem abrir sessão quando inválida.
- Conta existente e conta inexistente levam o mesmo tempo aproximado de resposta, para não revelar qual e-mail existe.
- Encerrar uma sessão já encerrada ou expirada responde "não autenticado" (401) e não causa erro interno.
- Banco indisponível durante o login responde erro de dependência indisponível no formato padrão, sem expor texto de SQL, e sem abrir sessão.
- Reinício do servidor não pode transformar sessões vencidas ou encerradas em válidas.
- Conta de administrador autentica normalmente, mas isso não lhe dá acesso irrestrito aos documentos das pacientes (regras de perfil são da RC-05).
- Tentativas repetidas de senha errada: a proteção contra força bruta é uma limitação conhecida desta história, registrada na documentação (ver Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A API MUST autenticar uma conta ativa a partir de e-mail e senha e, em caso de sucesso, abrir uma sessão e devolver o token, o instante de expiração e os dados básicos da conta (nome e perfil).
- **FR-002**: A API MUST responder com o mesmo erro genérico de credenciais inválidas para senha errada, e-mail inexistente e conta desativada, sem abrir sessão e sem indicar qual dado falhou.
- **FR-003**: A API MUST validar o corpo do login (e-mail e senha presentes e legíveis) e responder requisição inválida com os campos com problema, sem abrir sessão.
- **FR-004**: A senha MUST ser verificada contra o hash armazenado com algoritmo próprio para senhas, de biblioteca consolidada, compatível com os hashes BCrypt custo 12 carregados pela RC-03; MUST NOT haver texto puro, criptografia reversível nem hash rápido caseiro.
- **FR-005**: As contas de paciente A, paciente B e administrador da carga sintética MUST autenticar com a senha de demonstração documentada, sem alterar a carga existente.
- **FR-006**: Toda sessão MUST ter prazo de validade; depois do prazo, o token MUST ser recusado.
- **FR-007**: A API MUST permitir consultar a sessão atual (identidade e expiração) e encerrar a sessão; depois de encerrada, o mesmo token MUST ser recusado imediatamente.
- **FR-008**: Token ausente, malformado, desconhecido, expirado ou encerrado MUST resultar em 401 `NAO_AUTENTICADO` no formato padrão de erro, sem diferenciar publicamente o motivo.
- **FR-009**: Ao autenticar uma requisição, o sistema MUST identificar a conta pela sessão e MUST NOT aceitar identificador de conta ou de dona enviado livremente no corpo.
- **FR-010**: O token MUST ser imprevisível; o sistema MUST NOT guardar o token de forma que o vazamento dos dados armazenados permita usar as sessões.
- **FR-011**: Senha, hash de senha e token MUST NOT aparecer em respostas de erro, em logs, na documentação versionada nem no repositório; eventos de login registram apenas identificadores técnicos, resultado e código de erro.
- **FR-012**: Cada login bem-sucedido MUST abrir uma sessão independente; encerrar uma sessão MUST NOT encerrar as outras da mesma conta.
- **FR-013**: O contrato da API (rotas, corpos, códigos de resposta e erros), o OpenAPI, a coleção Postman e a documentação do app MUST ser atualizados com login, sessão atual e logout, incluindo o comportamento esperado do cliente (sem envio repetido durante o login, erro compreensível, novo login ao receber 401).
- **FR-014**: A autenticação MUST ser reutilizável pelas próximas histórias por um ponto único de verificação de sessão; esta história MUST NOT implementar perfis, isolamento por paciente nem rotas de negócio (RC-05 em diante).
- **FR-015**: O sistema MUST continuar a respeitar os limites do projeto: dados exclusivamente sintéticos, sem diagnóstico, sem definição de urgência e sem autorização de atendimento.

### Key Entities

- **Conta de usuário** (já existente na RC-03): e-mail único, nome, hash de senha, perfil (paciente ou administrador) e indicador de ativa; é a identidade que se autentica.
- **Sessão**: período em que uma conta está autenticada; tem identificador interno, conta dona, instante de criação, instante de expiração e indicação de encerrada; o token entregue à cliente é a única credencial da sessão e não é guardado de forma reutilizável.
- **Tentativa de login** (apenas como evento de registro, sem dado sensível): resultado e código de erro, sem senha nem token.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: As 3 contas da carga sintética (paciente A, paciente B e administrador) conseguem entrar com a senha de demonstração em 100% das tentativas de uma execução de verificação.
- **SC-002**: Em todos os casos de falha de credencial testados (senha errada, e-mail inexistente, conta desativada), as respostas são indistinguíveis entre si e nenhuma sessão é aberta (0 sessões criadas).
- **SC-003**: Depois de sair ou do prazo vencer, 100% das tentativas de usar o mesmo token em rota protegida são recusadas com 401.
- **SC-004**: Uma busca por senha de demonstração, hash e token nos logs gerados e nos arquivos versionados desta história encontra 0 ocorrências (exceto a senha de demonstração fictícia já documentada no README dos scripts da RC-03 e usada como constante nos testes).
- **SC-005**: Uma pessoa do grupo consegue, seguindo só a documentação, entrar, consultar a sessão e sair em menos de 5 minutos.
- **SC-006**: Duas sessões da mesma conta permanecem independentes em 100% das verificações (encerrar uma não afeta a outra).

## Assumptions

- A tela de login do aplicativo Android **não** está nesta história (decisão do responsável em 12/11/2026): o projeto móvel ainda não existe. A história entrega a API e deixa o comportamento da tela como requisito documentado (FR-013, História 4). A validação de que a tela o cumpre fica para a história que criar o app.
- Prazo padrão da sessão de **30 minutos** desde o login, sem renovação automática, configurável; é uma escolha demonstrativa, ajustável sem mudar o contrato. Sessão expirada pede novo login.
- A sessão é guardada no banco Oracle da faculdade, e isso pode exigir uma tabela nova com prefixo `RC_` criada por script aditivo (nunca alterando destrutivamente o esquema da RC-03); a decisão final de armazenamento é do plano.
- A senha de demonstração e os hashes são os da RC-03 (`database/scripts/README.md`); a compatibilidade dos hashes com a biblioteca de verificação (marcada como não verificada na RC-03) é verificada aqui.
- Fora do escopo: cadastro de conta, recuperação ou troca de senha, autenticação multifator, login social, perfis e isolamento por paciente (RC-05), proteção contra força bruta e bloqueio de conta após tentativas. A ausência dessa proteção é uma limitação conhecida desta etapa acadêmica e fica registrada na documentação.
- Exigência de HTTPS vale para execução remota autenticada; em desenvolvimento local o tráfego é HTTP, conforme `docs/ambiente.md`.
- O banco Oracle da faculdade é compartilhado e usa o prefixo `RC_`; testes só tocam objetos `RC_*` e nunca outras tabelas.
