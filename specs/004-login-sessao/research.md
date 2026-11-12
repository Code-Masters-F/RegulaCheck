# Pesquisa e decisões — RC-04

## D1. Biblioteca de senha
- **Decisão:** `at.favre.lib:bcrypt` 0.10.2 (consolidada; é a candidata registrada na RC-03). Verificação com `BCrypt.verifyer().verify(char[] senha, byte[] hash)`; o `Result` distingue formato inválido (`validFormat == false`) de senha errada (`verified == false`).
- **Rationale:** a documentação (Context7, consulta de 12/11/2026) confirma a API; lê `$2a$` e `$2a$12$` sem configuração. Formato inválido do hash armazenado é tratado como falha de login e registrado como erro técnico (sem o hash).
- **Alternativas:** Argon2 (aceito pela constituição, mas exigiria regerar os hashes da RC-03); `PBKDF2` do JDK (não é BCrypt, incompatível com os hashes carregados).
- **Senha longa:** o verificador padrão é estrito e lança exceção acima de 72 bytes (limite do BCrypt). A API recusa antes de chamar a biblioteca: senha com mais de 72 bytes UTF-8 vira o mesmo erro genérico de credenciais inválidas (nenhuma conta tem senha tão longa).

## D2. Driver e pool JDBC
- **Decisão:** `ojdbc11` 23.26.4.0.0 (já verificado em 12/11/2026) e `HikariCP` 6.3.0 com `maximumPoolSize=3`, `connectionTimeout=5000 ms` e `initializationFailTimeout=-1`.
- **Rationale:** a documentação do HikariCP define que valor menor que zero em `initializationFailTimeout` inicia o pool sem tentar conectar, o que preserva a regra "a API sobe sem banco". Tamanho 3 respeita o banco compartilhado.
- **Alternativas:** Oracle UCP (mais pesado e específico); conexão por requisição (lento); pool manual (arriscado).
- **Risco:** versões do pom precisam resolver no Maven; confirmar com `mvnw dependency:resolve` no início da implementação.

## D3. Formato do token e guarda no banco
- **Decisão:** token de 32 bytes de `SecureRandom`, codificado em Base64 URL sem preenchimento (43 caracteres). No banco fica só `SHA-256` do token em hexadecimal (`CHAR(64)`, único). O cliente envia `Authorization: Bearer <token>`.
- **Rationale:** o token tem 256 bits de entropia, então SHA-256 rápido é adequado (hash lento é para senhas de baixa entropia); vazamento da tabela não permite usar sessões (FR-010). Não usa JWT: encerrar a sessão precisa invalidar de imediato, e sessão no servidor torna isso trivial.
- **Alternativas:** JWT assinado (revogação difícil, exige gestão de chave); `HttpSession` do Jetty (cookie, em memória, perde sessões ao reiniciar e não serve ao app nativo).

## D4. Prazo da sessão
- **Decisão:** 30 minutos fixos desde o login, sem renovação, configurável por `SESSION_TTL_MINUTES` (inteiro entre 1 e 1440; inválido impede iniciar com mensagem clara). Instantes gravados em UTC.
- **Rationale:** simples, previsível e suficiente para demonstração; "sessão expirada pede novo login" é testável. Renovação deslizante fica como evolução (não pedida).
- **Relógio:** serviços recebem um `java.time.Clock` para testes determinísticos de expiração.

## D5. Tabela de sessões
- **Decisão:** nova `RC_SESSAO_LOGIN` criada por `05_sessao_login.sql` (aditivo, falha com ORA-00955 se já existir). Nome distinto de `RC_SESSAO_CAPTURA` (sensores) para não confundir.
- **Rationale:** a RC-03 já foi mesclada e verificada; mudar o `01_` reabriria a história. Script novo preserva o histórico e o princípio "sem DDL destrutivo". O `90_` ganha `RC_SESSAO_LOGIN` na lista (primeira a ser removida, por depender de `RC_USUARIO`); a guarda `'NAO'` permanece.
- **Limpeza de sessões vencidas:** fora do escopo (volume mínimo); registrado como pendência de manutenção.

## D6. Rotas e códigos
- `POST /auth/login` → 200 / 400 / 401 `CREDENCIAIS_INVALIDAS` / 415 / 503.
- `GET /auth/sessao` → 200 / 401 `NAO_AUTENTICADO`.
- `POST /auth/logout` → 204 / 401 `NAO_AUTENTICADO`.
- **Rationale:** `CONTRATO_API.md` já planejava `/auth/login`; `sessao` e `logout` completam o aceite. `CREDENCIAIS_INVALIDAS` é código novo (login falho não é "sessão ausente"); `NAO_AUTENTICADO` continua só para token ausente/expirado/inválido.
- **Logout repetido:** a segunda chamada com o mesmo token recebe 401 (o token já não vale). O efeito é idempotente: nada quebra.

## D7. Erro único para falha de credencial e equalização de tempo
- **Decisão:** senha errada, e-mail inexistente, conta desativada e senha longa demais devolvem **o mesmo** 401 `CREDENCIAIS_INVALIDAS`, mesma mensagem e mesmo corpo. Quando o e-mail não existe, o serviço verifica a senha contra um hash BCrypt de custo 12 "falso" fixo, para o tempo de resposta não revelar a existência do e-mail. Para conta desativada, a senha também é verificada antes de recusar.
- **Rationale:** FR-002 e a borda de enumeração de contas.

## D8. Normalização de entrada
- E-mail: `trim` e minúsculas (a carga RC-03 grava em minúsculas); tamanho máximo 254. Senha: obrigatória, sem trim, máximo 72 bytes UTF-8. Corpo máximo 4 KB. `Content-Type` deve ser `application/json` (senão 415 `TIPO_NAO_SUPORTADO`). JSON inválido ou campos ausentes/vazios: 400 `REQUISICAO_INVALIDA` com `detalhes` por campo; **nenhuma** sessão é aberta.

## D9. Ponto único de autenticação
- **Decisão:** `AutenticacaoFiltro` (Filter Jakarta) lê o `Authorization: Bearer`, pede ao `AutenticacaoService` para validar (hash do token → sessão ativa e não expirada → conta ativa) e grava um `SessaoAutenticada` (sessão, conta, perfil, expiração) como atributo da requisição; senão responde 401. Registrado nas rotas `/auth/sessao` e `/auth/logout`; as histórias seguintes o registram em suas rotas. Perfis não são aplicados aqui (RC-05).
- **Conta desativada depois do login:** a validação consulta `ATIVO` e recusa a sessão.
- **Identidade:** controllers leem a identidade do atributo da requisição, nunca do corpo (FR-009).

## D10. Banco indisponível ou não configurado
- Sem `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, ou com falha de conexão, as rotas de auth respondem 503 `DEPENDENCIA_INDISPONIVEL` (mensagem genérica, sem SQL) e nenhuma sessão é aberta. `GET /health` e a subida da API não mudam.
- `BancoDados` nunca imprime URL com usuário/senha; `Configuracao` sobrescreve `toString` para ocultar a senha.

## D11. Logs
- Eventos de login registram `resultado` (sucesso/falha), `codigo`, `usuarioId` quando a conta existe e duração; **não** registram e-mail, senha, hash nem token. Falhas inesperadas continuam via `RespostaErro` com `idRastreamento`.

## D12. Testes
- **Sem banco (no `clean verify`):** `VerificadorSenhaTest`, `GeradorTokenTest`, `AutenticacaoServiceTest` (repositórios em memória + `Clock` fixo), `AuthEndpointTest` (Jetty real, porta livre: login 200, falha idêntica nos 4 casos, 400, 415, sessão, logout, reuso 401, expiração, duas sessões independentes, token em log/corpo) e `HashesRc03CompatibilidadeTest` (lê os 3 hashes de `02_carga_sintetica.sql` e verifica a senha de demonstração fictícia; fecha o ponto "não verificado" da RC-03).
- **Oracle opt-in:** `AuthOracleTest`, com nome terminado em `Test` porque o pom só tem o *surefire* (que não executa `*IT`) e plugin novo seria dependência extra sem necessidade; usa `assumeTrue` nas variáveis `DB_*` e em `RC_TESTE_ORACLE=SIM`. Roda as 3 contas reais, grava e invalida sessões, simula reinício montando um serviço novo sobre o mesmo banco; remove apenas as sessões criadas por ele (por hash do token), nunca outras linhas.
- **Injeção em testes:** `Composicao.montar(Configuracao, Dependencias)` e `Servidor.criar(Configuracao, Dependencias)` recebem repositórios e `Clock`; o `Servidor.criar(Configuracao)` atual monta as dependências reais, então os testes existentes não mudam. Os testes com Jetty injetam os repositórios em memória por esse caminho.
- **Log:** o teste de higiene de log define `org.slf4j.simpleLogger.logFile` antes de iniciar o logger e lê o arquivo gerado.

## D13. O que fica fora (declarado)
- Tela Android (decisão do responsável), perfis e isolamento (RC-05), proteção contra força bruta, troca/recuperação de senha, renovação de sessão e purga de sessões vencidas.

## D14. Compatibilidade com Windows e CRLF
- Os scripts SQL seguem o padrão da RC-03 (`;` e `/`); o executor de verificação do `05_` reutiliza o executor temporário fora do repositório (scratchpad).
