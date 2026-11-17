# Research — RC-18

Decisões que a #40/#41 não fixam. As fixadas lá (Kotlin, Compose M3, minSdk 26, OkHttp + kotlinx-serialization, Keystore sem security-crypto, tempos, sem retentativa, `sealed interface Tela`/`EstadoTela<T>`) não são repetidas.

## D1 — Versões
- **Decision**: Gradle 8.7, AGP 8.5.2, Kotlin 2.0.21 (plugins compose e serialization da mesma versão), Compose BOM 2024.09.03, activity-compose 1.9.2, lifecycle 2.8.6, core-ktx 1.13.1, OkHttp/MockWebServer 4.12.0, serialization-json 1.7.3, coroutines-test 1.8.1, JUnit 4.13.2, num `libs.versions.toml`.
- **Rationale**: atendem AGP 8.5+/Gradle 8.7+/Kotlin 2.0.x; todas aceitam compileSdk 34 (core-ktx 1.15 e activity 1.10 já exigem 35). Gradle 8.7 já estava no cache local.
- **Alternatives**: AGP 8.7/Gradle 8.9 (sem ganho para a história).

## D2 — Projeto de um módulo
- **Decision**: a raiz `mobile/app/` aplica `com.android.application`.
- **Rationale**: a #41 pede o Gradle em `mobile/app/`; evita `mobile/app/app/`.

## D3 — Navegação e ciclo de vida
- **Decision**: `Navegador` (ViewModel da Activity) guarda a pilha de `Tela`; cada entrada da pilha é um `ViewModelStoreOwner` próprio, limpo ao sair da tela ou ao encerrar a sessão.
- **Rationale**: sem biblioteca de navegação (#40), mas com estado preservado na rotação e sem dados de uma conta sobrando em memória depois do logout.

## D4 — Sessão cifrada
- **Decision**: chave AES-256 (`AndroidKeyStore`, GCM/NoPadding, IV gerado pelo Cipher) cifra um JSON `{token, nome, perfil, expiraEm}`; o resultado (IV + texto cifrado, Base64) fica em `SharedPreferences` privado. Falha ao decifrar = sem sessão (dado apagado).
- **Rationale**: nome e perfil vêm só do login; guardá-los cifrados evita chamar `GET /auth/sessao` (fora das rotas da RC-18). Expiração local vencida também apaga a sessão.
- **Testabilidade**: `CofreSessao` é interface (fronteira com o Keystore, que não existe em teste JVM); os testes usam um cofre em memória.

## D5 — 401 central
- **Decision**: o `ApiClient` trata 401 de rota privada chamando o callback de expiração do `GerenciadorSessao`; login e logout desligam esse tratamento (no login, 401 = credenciais inválidas; no logout, a sessão já está sendo encerrada).

## D6 — Erros
- **Decision**: `ErroApi` selado com: `SemConexao`, `TempoEsgotado`, `CredenciaisInvalidas`, `SessaoExpirada`, `SemPermissao`, `NaoEncontrado`, `Conflito(codigo, mensagem, retryAfterSegundos)`, `ArquivoRecusado(codigo, mensagem)`, `RequisicaoInvalida(codigo, mensagem, detalhes)` (400/422), `Indisponivel(status)` (5xx) e `RespostaInesperada(status)` (outro status ou JSON fora do contrato). `SocketTimeoutException` → tempo esgotado; outra `IOException` → sem conexão.
- **Rationale**: cobre a lista da #41; 400/422 e "inesperada" evitam derrubar o app com respostas não previstas.

## D7 — Ações da RC-19 nesta história
- **Decision**: botões de ações que dependem de rotas da RC-19 ("Novo pedido", "Anexar documentos", "Ler documentos", "Ver pendências", "Ver o que conferir", "Ver resultado", "Tentar de novo" da análise) aparecem com o rótulo do desenho e, ao tocar, mostram "Esta etapa ainda não está disponível nesta versão do app.". "Atualizar" (`PROCESSANDO`) funciona: consulta o pedido de novo.
- **Rationale**: a #41 exige os botões e a ação por estado, mas o CLAUDE.md proíbe simular funcionalidade; a RC-19 troca o aviso pela navegação real.

## D8 — Fontes e ícones
- **Decision**: Bricolage Grotesque e Nunito Sans como fontes variáveis (OFL, baixadas de `github.com/google/fonts`) em `res/font`, com a licença ao lado. Ícones Lucide (ISC) recriados como `ImageVector` a partir dos caminhos SVG, só os usados.
- **Rationale**: fontes empacotadas não dependem do Google Play Services; `material-icons-extended` é grande e não tem o traço Lucide do desenho.
- **Ajuste na verificação (17/11)**: a Bricolage usa o tamanho óptico (`opsz`) 96 em todos os títulos. Com `opsz` igual ao tamanho do texto, "Entrar" ficava ~10% mais largo que na prévia KulXG; com 96, mede 84 dp, como no desenho.

## D10 — Sem reuso de conexão ociosa
- **Decision**: `ConnectionPool(0, …)`: cada chamada abre conexão nova.
- **Rationale**: sem retentativa automática (#40), uma conexão ociosa já fechada pelo servidor (Jetty e o servidor de teste fecham após 30 s) fazia a consulta seguinte falhar como "sem conexão" — reproduzido no emulador e coberto por teste. O custo (um handshake TCP por chamada, sem TLS no desenvolvimento) é irrelevante para o volume do app.
- **Alternatives**: ligar `retryOnConnectionFailure` (contraria a #40); cabeçalho `Connection: close` (mesmo efeito, menos explícito).

## D9 — URL da API
- **Decision**: `BuildConfig.API_BASE_URL`, padrão `http://10.0.2.2:8080`, sobrescrita por `-PapiBaseUrl=`. `network_security_config` só em `src/debug` permite texto claro para `10.0.2.2`, `localhost` e `127.0.0.1`; o IP do aparelho físico entra por `-PapiBaseUrl` + a mesma configuração (documentado). Release sem texto claro.
