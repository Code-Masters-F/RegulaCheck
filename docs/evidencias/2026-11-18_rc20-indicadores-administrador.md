# RC-20 — Protocolo e indicadores do administrador (18/11/2026)

História RC-20 (#43, Épico 6 #40) · spec [`specs/024-app-admin-indicadores/`](../../specs/024-app-admin-indicadores/) · branch `024-app-admin-indicadores` (worktree, a partir da `main` em `cde997e`). Só texto: nenhuma captura de tela, PNG ou relatório de teste versionado.

## Verificação automática (executada)

Ambiente: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rcgt`; Gradle com o JDK 21 do Android Studio (`JAVA_HOME` só na sessão; no worktree, `ANDROID_HOME` apontando para o SDK porque `local.properties` não é versionado).

| Comando | Resultado |
|---|---|
| `backend/`: `./mvnw clean verify` | 786 testes, 1 falha, 36 ignorados. A única falha é a conhecida do ambiente: `SimuladorSensoresProcessoTest.modoSecoEReproduzivelEAsSequenciasTemOsValoresEsperados:41` (`docs/ambiente.md`, "Problemas comuns"). |
| — `IndicadorEndpointTest` | 7/7: administrador 200 com os números iguais aos dados de teste (7 pedidos, 1 em falha, 0/2/1 por resultado, top 5 R01, R02, R03, R04, R05 com R06 cortada); paciente 403 `ACESSO_NEGADO` sem nenhuma consulta ao repositório; sem token e token inválido 401 `NAO_AUTENTICADO`; POST do administrador 405; falha do banco 503 `DEPENDENCIA_INDISPONIVEL`; banco vazio 200 com zeros; corpo só com os campos do contrato e sem `@`, `Paciente`, `email`, `nome`, `pacienteId`, `caminho`, `arquivo`, `trecho`, `resumo`, `mensagem`, id do pedido criado nem mensagens das regras |
| — `IndicadorServiceTest` | 4/4: zeros em todos os grupos, grupo ausente vira zero, ordem total desc/código asc com corte em 5 e exclusão de total 0, protocolo ativo com as 6 regras na ordem |
| — `MatrizAdministradorTest`, `OrdemDeVerificacaoTest` (rota de teste da RC-05) | verdes, sem alteração |
| `backend/`: `./mvnw -q package -DskipTests` | `target/regulacheck-backend.jar` gerado (testes já executados no passo anterior) |
| `mobile/app/`: `./gradlew assembleDebug testDebugUnitTest` | BUILD SUCCESSFUL; 90 testes, 0 falhas; APK `build/outputs/apk/debug/regulacheck-app-debug.apk` |
| — `IndicadoresApiTest` | 3/3: `GET /admin/indicadores` com `Authorization: Bearer`, leitura do contrato (campo desconhecido ignorado), 403 → sem permissão sem encerrar a sessão, 401 → sessão encerrada |
| — `IndicadoresViewModelTest` | 3/3: carregando → sucesso; 503 sem repetição automática e "Tentar de novo" faz nova consulta; 403 fica como sem permissão |
| — `ServicoSessaoTest` | 10/10: administrador entra com perfil guardado e tela inicial E4.2; paciente sempre "Meus pedidos"; perfil desconhecido recusado, logout remoto e nada guardado |

## Verificação manual com o backend real (executada)

O backend com Oracle é iniciado pelo responsável; nesta história, o da branch 024 rodou em `http://localhost:8081` (o da branch 023 em `:8080`). Roteiro em [quickstart](../../specs/024-app-admin-indicadores/quickstart.md):

1. `GET /admin/indicadores` com token de administrador → 200; anotar os números.
2. Mesmo `GET` com token de paciente → 403; sem token → 401.
3. Emulador: administrador → E4.2 com os números do passo 1; "Sair" → login.
4. Paciente → "Meus pedidos", sem acesso a E4.2.

Tentativa de 18/11/2026, por volta de 14h40 (backend da branch 024 em `http://localhost:8081`, iniciado pelo responsável):

| Chamada | Resultado |
|---|---|
| `GET /health` | 200 `UP` |
| `GET /admin/indicadores` sem token | 401 `NAO_AUTENTICADO` (a rota existe nesta versão; sem ela a resposta seria 404) |
| `POST /auth/login` (administrador e paciente) | 503 `DEPENDENCIA_INDISPONIVEL`, repetido uma vez; o backend da branch 023 em `:8080` respondeu o mesmo, então o Oracle não estava acessível a nenhum dos dois |

APK gerado com `-PapiBaseUrl=http://10.0.2.2:8081`, ainda não instalado: o emulador `emulator-5554` é compartilhado com a outra branch e a instalação substitui o app dela.

Resultado da tentativa: não verificado (sem acesso ao Oracle).

### Segunda execução, por volta de 15h25, com o Oracle de volta

**API** (script local com as contas fictícias; tokens não exibidos, sessões encerradas no fim):

| Chamada | Resultado |
|---|---|
| `GET /admin/indicadores`, administrador | 200. Protocolo `PROTOCOLO-ACADEMICO-FICTICIO-v0.1`, 6 regras (RC-R01 a RC-R06, na ordem); `totalPedidos` 7; vigentes: sem pendências 1, com pendências 1, confira a leitura 4; `pedidosEmFalhaAnalise` 0; regras com mais pendências: RC-R03 4 (0 + 4), RC-R04 4 (1 + 3), RC-R05 4 (1 + 3), RC-R01 3 (0 + 3), RC-R02 3 (0 + 3) — empates em ordem de código, como o contrato. O corpo não contém `@`, `Paciente`, `email`, `nome`, `pacienteId` nem `caminho`; acentos chegam em UTF-8 (`Content-Type: application/json;charset=utf-8`) |
| `GET /admin/indicadores`, paciente | 403 `ACESSO_NEGADO` |
| `GET /admin/indicadores`, sem token | 401 `NAO_AUTENTICADO` |
| `GET /solicitacoes`, administrador | 403 `ACESSO_NEGADO` (o administrador continua sem rotas de paciente) |

Isso exercita o SQL de `IndicadorRepositoryJdbc` contra o Oracle: as três consultas rodaram sem erro e devolveram números coerentes entre si (6 análises vigentes para 7 pedidos). Os números não foram recontados por uma consulta SQL independente.

**App** (APK debug com `-PapiBaseUrl=http://10.0.2.2:8081`, instalado com `adb install -r` no `emulator-5554`, AVD `Medium_Phone`; textos lidos por `uiautomator dump` e uma captura só para conferência local, não versionada):

| Passo | Resultado |
|---|---|
| Abrir o app com a sessão de paciente que já estava guardada | "Meus pedidos" (tela do perfil paciente) |
| "Sair" → login → entrar como `admin@exemplo.invalid` | Abriu direto E4.2 "Protocolo e indicadores", sem passar por telas de paciente |
| Conteúdo de E4.2 | "Protocolo acadêmico fictício · versão 0.1 · 6 regras" e as 6 regras com código e nome; "Total de pedidos" 7; "Análises por resultado": Sem pendências identificadas 1, Com pendências 1, Confira a leitura 4, Falha 0, com o texto sobre a base das contagens; "Regras com mais pendências" com os mesmos 5 itens e quantidades da API; nota "Dados agregados de demonstração. Documentos e pedidos individuais não são exibidos."; rodapé acadêmico; "Sair". **Iguais à consulta direta à API** |
| "Sair" em E4.2 | Voltou ao login com e-mail e senha vazios |
| Entrar como `paciente.a@exemplo.invalid` | "Meus pedidos" (Olá, Paciente; pedidos 118, 112, 111, 4); nenhum botão ou caminho para E4.2 |
| `adb logcat -d` (78 mil linhas) | Nenhum token, cabeçalho `Bearer` ou corpo de resposta do app. As únicas linhas com a senha de demonstração são do `adbd`, o serviço do emulador que registra os próprios comandos `adb shell input text` usados para digitar no teste |

Observação do ambiente: o teclado do emulador (Gboard) pediu acesso a contatos ao focar o campo de e-mail; foi recusado ("Don't allow").

## Comparação com os quadros do pen (18/11/2026, por volta de 15h35)

Como foi feita: os quadros `T8XMZ` (padrão), `Ifg71` (carregando) e `mcN6S` (erro) de `design/RegulaCheck.pen` foram lidos pelo MCP do pen.dev (textos, componentes e posições, e imagens das seções só para conferência, sem exportar nada para o repositório e sem alterar o `.pen`). O estado padrão do app foi capturado no `emulator-5554` com o backend real; carregando e erro foram comparados pelo código de `IndicadoresTela.kt` (com o backend real o carregamento é rápido demais para capturar, e provocar o erro exigiria derrubar o backend ou a rede do emulador, compartilhados com a outra branch). Entre as duas capturas o total passou de 7 para 8 pedidos (outro trabalho em paralelo criou um pedido); a tela acompanhou a API.

| Elemento | Quadro | App | Situação |
|---|---|---|---|
| Cabeçalho | `Cabeçalho/Marca` com "Sair" | igual | igual |
| Título | "Protocolo e indicadores" (32/600), sem saudação | igual | igual |
| Nota "Dados agregados de demonstração. Documentos e pedidos individuais não são exibidos." | logo abaixo do título, como texto de apoio (14), nos três estados | caixa informativa no fim da tela, só no estado padrão | **diferente** |
| Protocolo ativo | `Dado/Linha`: rótulo "Protocolo ativo" (14/600, cinza) e "Protocolo acadêmico fictício · versão 0.1 · 6 regras" (16), sem cartão e sem as regras | cartão de superfície com título 18 e as 6 regras dentro | **diferente** |
| Total de pedidos | `Indicador/Total`: cartão de superfície, rótulo 14/600, número 40/600 e "Valor ilustrativo de demonstração." | título de seção 18 e número 32, sem cartão e sem a frase | **diferente** |
| Análises por resultado | título 18/600 e quatro `Dado/Linha` (rótulo 14/600 cinza em cima, número 16 embaixo), sem ícone; último rótulo "Falha na verificação" | rótulo com ícone de estado à esquerda, número à direita e divisória; último rótulo "Falha"; frase extra sobre a base das contagens (criada no clarify) | **diferente** |
| Regras com mais pendências | `Dado/Linha` ("RC-R04 · Motivo do encaminhamento informado" em cima, "7 pendências" embaixo); 3 itens | texto à esquerda, quantidade à direita, divisória; até 5 itens (FR-005) | **diferente** (formato e quantidade) |
| Regras do protocolo | seção própria depois das pendências, seis `Critério/Linha` (nome 16 e código 13 embaixo, divisória, sem resultado) | não há seção; as regras aparecem no cartão do protocolo como "RC-R01 · nome" | **diferente** |
| Rodapé | `Aviso/Rodapé` | igual | igual |
| Carregando | título, nota e `Carregando/Esqueleto` (dois blocos) com "Carregando indicadores…" | título e esqueleto do app (um bloco e três linhas) com "Carregando indicadores…", sem a nota | parecido; **falta a nota** |
| Erro | título, nota e `Retorno/Erro`: "Não foi possível carregar os indicadores.", "Tente de novo. Se o problema continuar, volte mais tarde." e "Tentar de novo" (contorno com ícone) | título e `BlocoRetorno` de erro com o título e "Tentar de novo" (contorno com ícone), sem o texto de apoio e sem a nota | **diferente** (faltam o texto de apoio e a nota) |

Conclusão: os textos, os números e os estados batem, mas a organização da tela padrão não segue o quadro `T8XMZ`: a nota está no lugar errado, o total e as linhas usam outros componentes, e as regras do protocolo deveriam ser uma seção própria depois das pendências. A tela foi montada a partir de `mobile/design/textos.md` sem abrir o quadro, ao contrário da orientação "não redesenhe". "Valor ilustrativo de demonstração." faz sentido no quadro, que mostra um número inventado; no app o número vem da API (agregado de dados sintéticos), por isso a frase não foi usada. Decisão do responsável em 18/11/2026: manter a tela como está (organização atual, sem a frase, até 5 regras); as diferenças acima ficam registradas e não são tratadas na RC-20.

## O que não foi verificado

- Recontagem independente dos números por SQL direto no Oracle (o `.env` não é lido; só a API foi consultada).
- Estados carregando e erro de E4.2 com o backend real (cobertos só pelos testes de ViewModel); o carregamento foi rápido demais para ser lido no dump.
- O tempo até a tela (SC-004) não foi cronometrado; a tela apareceu dentro da espera de 5 s do roteiro.
- Carregando e erro de E4.2 renderizados no aparelho (comparados com os quadros só pelo código).
- TalkBack e fonte ampliada (fora do épico #40).
