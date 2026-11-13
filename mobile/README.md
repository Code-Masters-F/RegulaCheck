# Mobile

**Android nativo com Kotlin** (decidido). **Projeto ainda não criado**: esta pasta só reserva o lugar e documenta como o app alcança a API.

Telas previstas: login, lista de solicitações, rascunho, envio de documentos, conferência da transcrição, resultado da análise, submissão simulada.

Contrato: o app consome somente a API do backend ([contratos](../docs/api/contratos-dados.md), [erros](../docs/api/erros.md), [estados](../docs/api/estados-e-revisoes.md), [rotas](../CONTRATO_API.md)). Nenhuma credencial de banco ou IA no app.

## Login e sessão (RC-04): comportamento esperado da tela
A API de autenticação existe (`POST /auth/login`, `GET /auth/sessao`, `POST /auth/logout`; contrato em [specs/004-login-sessao/contracts/api-auth.md](../specs/004-login-sessao/contracts/api-auth.md)), mas **a tela e o armazenamento do token serão implementados na história que criar o app**. Quando existir, a tela de login deve:
1. Ao tocar em *Entrar*, desabilitar o botão e mostrar progresso até a resposta; ignorar toques repetidos enquanto houver envio em andamento.
2. Em 401 `CREDENCIAIS_INVALIDAS`, mostrar a mensagem recebida, manter o e-mail digitado, limpar a senha e reabilitar o botão.
3. Em 503 ou falha de rede, mostrar "serviço indisponível, tente novamente" sem abrir sessão e preservar o e-mail.
4. Guardar o token só no armazenamento seguro da plataforma Android (nunca em log, arquivo público ou texto comum) e enviá-lo em `Authorization: Bearer`.
5. Em qualquer 401 `NAO_AUTENTICADO`, apagar o token e voltar ao login pedindo novo acesso.
6. Ao sair, chamar `POST /auth/logout` e apagar o token local, mesmo que a chamada falhe por rede.

## Respostas de autorização (RC-05): comportamento esperado do app
- **401** `NAO_AUTENTICADO`: apagar o token e voltar ao login (já descrito acima).
- **403** `ACESSO_NEGADO`: mostrar "sem permissão para esta tela"; não tentar de novo automaticamente. A paciente não deveria ver telas administrativas.
- **404** `NAO_ENCONTRADO`: mostrar "item não encontrado" e voltar à lista; é a mesma resposta para um item que não existe e para um de outra paciente.
- O app nunca envia identificador de dona no corpo e nunca guarda ou usa a chave do simulador (`IOT_API_KEY`), que pertence ao script de sensores.

## Segurança do app (RC-06): comportamento esperado
- **HTTPS fora do desenvolvimento local:** na demonstração remota o endereço da API é `https://...`. Tráfego em texto claro (`http://`) só na variante de **depuração**, restrito a `10.0.2.2`, ao IP do computador na rede local e a `localhost`, por configuração de segurança de rede do Android; a variante de entrega não permite texto claro.
- **Nenhum segredo no app além do token de sessão:** sem credencial de banco, sem `IOT_API_KEY`, sem chave de IA (a IA é mockada) e sem senha guardada. O token fica no armazenamento seguro da plataforma e nunca em log.
- **Exibir texto como texto:** o resumo simulado chega com `simulado: true` e `formato: "texto_puro"`; mostre-o em componente de texto, **nunca** em WebView nem interpretando HTML. Identifique-o como simulado.
- **Arquivos:** limites 3 arquivos, 5 MB cada, PDF até 5 páginas, JPEG/PNG. Em recusa (400/413/415), mostre a mensagem recebida, preserve o que a pessoa digitou e explique que é preciso escolher o arquivo de novo. O download vem como anexo autenticado; não guarde o arquivo em área pública.
- A varredura de segredos do repositório também cobre `mobile/`.

## Design das telas (pen.dev)
O desenho das telas do app é feito no [pen.dev](https://www.pen.dev/) (arquivos `.pen`), com **dados fictícios** e **sem chamar a API**. Fica em [`design/`](design/): `00-fundamentos.pen` (cores, tipografia, componentes), `01-entrada.pen`, `02-envio.pen`, `03-resultado.pen` e `04-sensores-admin-acessibilidade.pen`.
- **Arquivos `.pen`:** abra e edite só pelo pen.dev e suas ferramentas; não edite à mão.
- **Texto revisável no Git:** [`design/INDEX.md`](design/INDEX.md) (quadro → rota da API → contrato), [`design/textos.md`](design/textos.md) (todos os textos das telas), [`design/dados-ficticios.md`](design/dados-ficticios.md), [`design/mapa-de-percursos.md`](design/mapa-de-percursos.md), [`design/acessibilidade.md`](design/acessibilidade.md) e [`design/checklist-revisao.md`](design/checklist-revisao.md).
- **Regras de conteúdo:** tudo identificado como acadêmico e sintético; resumo e leitura marcados **SIMULADO**; nenhum texto afirma diagnóstico, urgência, autorização, vaga ou aprovação do SUS; estado nunca só por cor.
- **Situação:** os documentos de apoio existem; **os quadros ainda não foram desenhados** (ver o épico em `specs/007-epico-telas-pendev/`).

## Endereço da API em desenvolvimento
| Onde o app roda | Endereço |
|---|---|
| Emulador Android | `http://10.0.2.2:8080` |
| Aparelho físico (mesma rede do computador) | `http://<IP-do-computador>:8080` |

`localhost` **não** funciona no emulador nem no aparelho. Detalhes, firewall e a nota sobre HTTP em desenvolvimento: [docs/ambiente.md](../docs/ambiente.md#acesso-do-app-à-api).
