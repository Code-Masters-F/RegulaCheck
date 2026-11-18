# Índice das telas

> **Fonte:** [specs/015-fluxo-telas-app/contracts/inventario-quadros.md](../../specs/015-fluxo-telas-app/contracts/inventario-quadros.md). As ações de cada tela estão em [telas-operacoes.md](../../specs/015-fluxo-telas-app/contracts/telas-operacoes.md), os textos em [textos.md](textos.md), os dados em [dados-ficticios.md](dados-ficticios.md) e os percursos em [mapa-de-percursos.md](mapa-de-percursos.md).
>
> **Situação em 14/11/2026:** os 94 quadros estão desenhados no `design/RegulaCheck.pen` e foram revisados por quem desenhou: screenshot, consulta de limites, aviso acadêmico, selo SIMULADO e no máximo uma ação principal. Nenhum foi **aprovado** pelo grupo, e nenhum passou pelo percurso com uma pessoa que não desenhou (T060). Nenhuma tela chama a API: a coluna "Operação" diz o que o app vai usar. † indica rota contratada e ainda não implementada no backend; esses quadros levam a nota "contrato provisório".

Legenda de situação: *desenhado* → *revisado* (por quem desenhou) → *aprovado* (pelo grupo). ID: quadro no `design/RegulaCheck.pen`, aberto pelo pen.dev (as prévias PNG foram retiradas do repositório em 18/11/2026). Na coluna "Operação", † marca rota contratada e ainda não publicada; as rotas de análises (RC-13) estão na `main` desde 15/11/2026 e perderam a marca aqui, mas os quadros no `.pen` ainda a mostram até a T073.

## Como o canvas está organizado (research D20)

- **Topo, à esquerda (x=0):** F1–F4, o design system v1.0.
- **À direita (x=5200):**
  - O cartão **"Como avaliar o fluxo"** (`io4cw`) explica a leitura e numera o caminho principal da demonstração.
  - Abaixo dele, **8 etapas** com **uma linha por tela (01 a 21)**, na ordem de uso.
- **Em cada linha:**
  - O cartão à esquerda diz o propósito da tela, de onde se chega, para onde se vai e a operação.
  - Os estados vão da esquerda para a direita, começando pelo mais comum; na linha 03 (E1.3), seguem o ciclo de vida do pedido.
  - Cada quadro tem a sua nota logo abaixo.

| Quadro | ID | Conteúdo |
|---|---|---|
| F1 · Fundamentos · v1.0 | `IkCIw` | cor, tipografia, espaço, forma e toque |
| F2 · Componentes · v1.0 | `t9lvSH` | catálogo de 71 componentes |
| F3 · Confiança e simulação · v1.0 | `EVIof` | aviso acadêmico (rodapé e balão completo), aviso de proposta, selo SIMULADO, resumo e comprovante |
| F4 · Estados do pedido · v1.0 | `HBByt` | 7 estados do pedido, 4 do arquivo e 4 resultados por critério |

Regras do design system: [design-system.md](design-system.md). Acessibilidade: [acessibilidade.md](acessibilidade.md) e linha 21 (E4.4).

## Etapa 1 · Entrar e ver os pedidos (RC-36)

### 01 · E1.1 Login

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E1.1 Login — Padrão | `KulXG` | `POST /auth/login` | revisado |
| E1.1 Login — Enviando | `ht04w` | `POST /auth/login` | revisado |
| E1.1 Login — Credenciais inválidas | `o1mx3h` | `POST /auth/login` | revisado |
| E1.1 Login — Serviço indisponível | `r5kJX` | `POST /auth/login` | revisado |
| E1.1 Login — Sem conexão | `V9Q1Ku` | `POST /auth/login` | revisado |

### 02 · E1.2 Meus pedidos

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E1.2 Meus pedidos — Sucesso | `H0BJi` | `GET /auth/sessao`, `GET /solicitacoes`; Sair: `POST /auth/logout` | revisado |
| E1.2 Meus pedidos — Carregando | `HX74z` | `GET /auth/sessao`, `GET /solicitacoes`; Sair: `POST /auth/logout` | revisado |
| E1.2 Meus pedidos — Vazia | `v6aVEO` | `GET /auth/sessao`, `GET /solicitacoes`; Sair: `POST /auth/logout` | revisado |
| E1.2 Meus pedidos — Erro | `FzpeX` | `GET /auth/sessao`, `GET /solicitacoes`; Sair: `POST /auth/logout` | revisado |
| E1.2 Meus pedidos — Sem conexão | `MKpMg` | `GET /auth/sessao`, `GET /solicitacoes`; Sair: `POST /auth/logout` | revisado |

## Etapa 2 · O pedido em cada situação (RC-36)

### 03 · E1.3 Pedido

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E1.3 Pedido — Rascunho sem documentos | `hC19p` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Rascunho sem leitura | `xRLu9` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Rascunho a conferir | `Z3WVJ` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Rascunho pronto para verificar | `LP6Ni` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Em verificação | `ppR8m` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Confira a leitura | `V2Gp00` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Com pendências | `DGEB7` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Sem pendências identificadas | `rr205` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Falha na verificação | `a6MB6X` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Resultado não vigente | `STo2u` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Simulação concluída | `jGzFh` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Carregando | `toyoT` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |
| E1.3 Pedido — Não encontrado | `wgjxU` | `GET /solicitacoes/{id}`, documentos, extrações e conferência; análises | revisado |

## Etapa 3 · Montar o pedido e enviar documentos (RC-37 e RC-39)

### 04 · E2.1 Novo pedido

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E2.1 Novo pedido — Padrão | `c78REx` | `GET /protocolos`, `POST /solicitacoes`, `PUT /solicitacoes/{id}` | revisado |
| E2.1 Novo pedido — Criando | `gj4eA` | `GET /protocolos`, `POST /solicitacoes`, `PUT /solicitacoes/{id}` | revisado |
| E2.1 Novo pedido — Erro | `Zk97Z` | `GET /protocolos`, `POST /solicitacoes`, `PUT /solicitacoes/{id}` | revisado |
| E2.1 Novo pedido — Sem serviços | `pEOeQ` | `GET /protocolos`, `POST /solicitacoes`, `PUT /solicitacoes/{id}` | revisado |
| E2.1 Novo pedido — Trocar serviço | `V5J0Uz` | `GET /protocolos`, `POST /solicitacoes`, `PUT /solicitacoes/{id}` | revisado |

### 05 · E2.2 Seus documentos

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E2.2 Documentos — Vazia | `t31hN` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Enviando | `hHwfV` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Com arquivos | `CvQRm` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Não enviado | `WPH7O` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Opções do documento | `OndkJ` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Limite atingido | `Leg44` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Pedido verificado | `FEqIA` | `GET` e `POST …/documentos` | revisado |
| E2.2 Documentos — Pedido concluído | `vRJhG` | `GET` e `POST …/documentos` | revisado |

### 06 · E2.7 Anexar

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E2.7 Anexar — Tipo do documento | `fwOMP` | escolha local; depois `POST …/documentos` | revisado |
| E2.7 Anexar — Origem | `ncnpi` | escolha local; depois `POST …/documentos` | revisado |

### 07 · E4.1 Condições da captura

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E4.1 Condições da captura — Normal | `o3kkGI` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |
| E4.1 Condições da captura — Luminosidade fora | `fOrmB` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |
| E4.1 Condições da captura — Distância fora | `NyDDm` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |
| E4.1 Condições da captura — Recuperada | `DGf2T` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |
| E4.1 Condições da captura — Desatualizada | `Cna25` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |
| E4.1 Condições da captura — Indisponível | `kaHJz` | `GET /solicitacoes/{id}/leituras` †; foto: `POST …/documentos` | revisado |

### 08 · E2.3 Recusa de arquivo

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E2.3 Recusa de arquivo — Formato não suportado | `mNcfi` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — Extensão incompatível | `Kg6LK` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — Acima de 5 MB | `b7rGX` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — PDF com mais de 5 páginas | `oevl9` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — PDF protegido | `n2IevX` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — Arquivo ilegível | `KgK0r` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — Limite de 3 | `EvfCq` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |
| E2.3 Recusa de arquivo — Pedido não aceita documentos | `pHaNP` | recusas de `POST …/documentos` (415, 413, 400, 422, 409) | revisado |

### 09 · E2.4 a E2.6 Substituir, remover e excluir

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E2.4 Substituir documento — Rascunho | `LWEDT` | `PUT …/documentos/{docId}` | revisado |
| E2.4 Substituir documento — Pedido verificado | `thP1B` | `PUT …/documentos/{docId}` | revisado |
| E2.5 Remover documento — Rascunho | `Wlm4C` | `DELETE …/documentos/{docId}` | revisado |
| E2.5 Remover documento — Pedido verificado | `p9txyW` | `DELETE …/documentos/{docId}` | revisado |
| E2.6 Excluir rascunho | `Y92Uz` | `DELETE /solicitacoes/{id}` | revisado |

## Etapa 4 · Ler e conferir os documentos (RC-38)

### 10 · E3.1 Leitura simulada

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.1 Leitura simulada — Lendo | `QxKN9` | `POST …/extracoes` | revisado |
| E3.1 Leitura simulada — Indisponível | `PbHSM` | `POST …/extracoes` | revisado |
| E3.1 Leitura simulada — Sem documentos | `T1Kav` | `POST …/extracoes` | revisado |
| E3.1 Leitura simulada — Documentos mudaram | `aEuZ4` | `POST …/extracoes` | revisado |

### 11 · E3.2 Conferência

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.2 Conferência — Documento lido | `pJHml` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Decisões registradas | `gHSkA` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Não localizado indicado | `ivjGg` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Campo ilegível | `pKxnU` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Documento não lido | `n2kV6` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Texto do documento como dado | `lPe5I` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Desatualizada | `tr1Ql` | `GET` e `PUT …/conferencia` | revisado |
| E3.2 Conferência — Erro ao salvar | `h94G7s` | `GET` e `PUT …/conferencia` | revisado |

## Etapa 5 · Verificar critérios e corrigir (RC-38)

### 12 · E3.8 Verificar critérios

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.8 Verificar critérios — Confirmação | `Xe5GF` | `POST …/analises` | revisado |

### 13 · E3.3 Resultado

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.3 Resultado — Com pendências | `L5Keg` | `GET …/analises`; resumo † | revisado |
| E3.3 Resultado — Confira a leitura | `zMx9T` | `GET …/analises`; resumo † | revisado |
| E3.3 Resultado — Sem pendências identificadas | `cAl1Z` | `GET …/analises`; resumo † | revisado |
| E3.3 Resultado — Falha | `EAe9t` | `GET …/analises`; resumo † | revisado |
| E3.3 Resultado — Resumo indisponível | `udibQ` | `GET …/analises`; resumo † | revisado |

### 14 · E3.4 Detalhe da pendência

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.4 Pendência — Unidade emissora | `XCNVm` | `GET …/analises`; `GET` e `PUT …/documentos/{docId}` | revisado |
| E3.4 Detalhe da pendência — Substituir arquivo | `QQLpq` | `GET …/analises`; `GET` e `PUT …/documentos/{docId}` | revisado |
| E3.4 Detalhe da pendência — Conferir leitura | `HK8ZN` | `GET …/analises`; `GET` e `PUT …/documentos/{docId}` | revisado |
| E3.4 Detalhe da pendência — Documento ausente | `u8AYS3` | `GET …/analises`; `GET` e `PUT …/documentos/{docId}` | revisado |

### 15 · E3.5 Correção

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.5 Correção — Resultado não vigente | `LGYSw` | `PUT …/documentos/{docId}`, `POST …/extracoes`; análises | revisado |
| E3.5 Correção — Comparação | `vwCYD` | `PUT …/documentos/{docId}`, `POST …/extracoes`; análises; comparação † (RC-16) | revisado |

## Etapa 6 · Concluir a simulação (RC-38)

### 16 · E3.7 Concluir simulação

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.7 Concluir simulação — Confirmação | `ed21u` | `POST …/submissoes-simuladas` † | revisado |

### 17 · E3.6 Comprovante simulado

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E3.6 Comprovante simulado | `qRdi3` | `POST …/submissoes-simuladas` † | revisado |

## Etapa 7 · Coordenação (RC-39)

### 18 · E4.2 Protocolo e indicadores

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E4.2 Protocolo e indicadores — Padrão | `T8XMZ` | `GET /admin/indicadores` †; Sair: `POST /auth/logout` | revisado |
| E4.2 Protocolo e indicadores — Carregando | `Ifg71` | `GET /admin/indicadores` †; Sair: `POST /auth/logout` | revisado |
| E4.2 Protocolo e indicadores — Erro | `mcN6S` | `GET /admin/indicadores` †; Sair: `POST /auth/logout` | revisado |

## Etapa 8 · Situações em qualquer tela (RC-36 e RC-39)

### 19 · E1.4 e E1.5 Sessão e permissão

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E1.4 Sessão expirada | `s25M7` | 401 de qualquer rota privada | revisado |
| E1.5 Sem permissão | `pijJA` | 403 de qualquer rota privada | revisado |

### 20 · E4.3 Conexão ruim

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E4.3 Conexão ruim — Tempo esgotado | `ZPo7y` | falha de rede em `POST /solicitacoes` e `POST …/documentos` | revisado |
| E4.3 Conexão ruim — Sem conexão | `j9cJY` | falha de rede em `POST /solicitacoes` e `POST …/documentos` | revisado |

### 21 · E4.4 Acessibilidade

| Estado (esquerda → direita) | ID | Operação | Situação |
|---|---|---|---|
| E4.4 Acessibilidade — Anotações | `YapQA` | — (anotações e variações de E1.2, E2.2 e E3.3) | revisado |
| E4.4 Acessibilidade — Fonte ampliada E1.2 | `z7WVDk` | — (anotações e variações de E1.2, E2.2 e E3.3) | revisado |
| E4.4 Acessibilidade — Fonte ampliada E2.2 | `OzUVO` | — (anotações e variações de E1.2, E2.2 e E3.3) | revisado |
| E4.4 Acessibilidade — Fonte ampliada E3.3 | `VJC8f` | — (anotações e variações de E1.2, E2.2 e E3.3) | revisado |

**Total: 94 quadros em 21 linhas e 8 etapas**, iguais ao inventário. A numeração das linhas (01–21) é a mesma do canvas e do cartão "Como avaliar o fluxo".

## Ordem de leitura e rótulos

Padrão de todas as telas: cabeçalho e contexto → título → conteúdo e próxima ação → ações → aviso acadêmico. A ordem detalhada e os rótulos para leitor de tela de E1.2, E2.2 e E3.3 estão no quadro `YapQA` (linha 21, E4.4 Anotações). Ícones sem texto ("Voltar", "Mais opções") têm rótulo próprio e área de toque de 48. Nada disso foi verificado com TalkBack.
