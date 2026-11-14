# Data Model: Épico de telas do aplicativo no pen.dev

**Não há tabelas, colunas, scripts SQL, classes Java nem rotas novas.** (Total do épico: 73 quadros; matriz tela × estado em `contracts/telas.md`.) Este documento define as convenções dos artefatos de design.

## Artefatos

**Atualização de 13/11/2026:** para a transposição dos três exemplos aprovados, usar `design/RegulaCheck.pen` e os IDs registrados em `design/README.md`. A organização em cinco arquivos abaixo é histórica; o inventário completo de 73 estados ainda não foi entregue.

| Artefato | Formato | Papel |
|---|---|---|
| `mobile/design/NN-*.pen` | pen.dev (o site o descreve como JSON aberto; acessar só por ferramentas do pen.dev) | desenho das telas |
| `INDEX.md` | Markdown | quadro → rota da API → contrato → RC de origem; rótulos de leitor de tela e ordem de leitura |
| `textos.md` | Markdown | textos da interface por tela e estado |
| `dados-ficticios.md` | Markdown | conjunto único de dados inventados |
| `mapa-de-percursos.md` | Markdown + Mermaid | percursos e ligações |
| `acessibilidade.md` | Markdown | metas e lista de verificação |
| `checklist-revisao.md` | Markdown | checklist e registro da revisão |
| `previas/*.png` | PNG (opcional) | prévia de cada quadro, **se** o pen.dev exportar (D8) |

## Convenção de nomes de quadros

`<ID> <Tela> — <Estado>`

- **ID**: `F` fundamentos · `E1` entrada · `E2` envio · `E3` resultado · `E4` sensores/administrador/acessibilidade, seguido de `.N` (número da tela no grupo). Ex.: `E1.2`.
- **Estado** (lista fechada): Padrão · Carregando · Vazia · Sucesso · Erro · Sem conexão · Sessão expirada · Sem permissão · Não encontrado · mais estados próprios da tela (ex.: Enviando, Falhou, Confirmação).
- Exemplo: `E1.2 Lista de pedidos — Vazia`.

## Variáveis de design (arquivo `00-fundamentos`)

| Grupo | Variáveis |
|---|---|
| Cor | primária, secundária, superfície, fundo, texto, texto secundário, borda, sucesso, atenção, erro, informação, simulado (todas com pares de contraste ≥ 4,5:1 com o texto que carregam) |
| Tipografia | título, subtítulo, corpo, legenda, botão (corpo base ≥ 16 sp) |
| Espaço | escala 4/8/12/16/24/32 dp |
| Forma | raios de cartão, botão e campo |
| Toque | altura mínima de alvo 48 dp |

## Componentes (arquivo `00-fundamentos`)

Botão (primário, secundário, destrutivo, desabilitado, carregando) · campo de texto (padrão, foco, erro, desabilitado) · seletor · cartão de pedido · **selo de estado** (texto + ícone para os sete estados) · cartão de pendência · linha de documento (com estado do envio) · linha de campo extraído (identificado, não localizado, ilegível) · medidor de sensor (lux/cm) · **aviso acadêmico** · **selo SIMULADO** · faixa de erro e de sem conexão · diálogo de confirmação · esqueleto de carregamento · estado vazio · barra superior · navegação inferior · cartão de total (administrador).

## Dados fictícios (resumo; detalhe em `dados-ficticios.md`)

| Item | Regra |
|---|---|
| Pessoas | Paciente A e Paciente B (nomes inventados), administrador; e-mails `.invalid` |
| Pedidos | um por estado (`RASCUNHO` … `SUBMETIDA_SIMULACAO`), serviço "Consulta ambulatorial em cardiologia (contexto documental)", Município Exemplo (fictício) |
| Documentos | um PDF de 2 páginas e uma foto; tamanhos e nomes inventados |
| Extração | campos do protocolo com os três status (identificado, não localizado, ilegível) e origem (arquivo/página/trecho) |
| Pendências | uma por regra do `PROTOCOLO-ACADEMICO-FICTICIO v0.1`, com responsável (paciente ou unidade emissora) |
| Sensores | lux e cm em faixa, fora da faixa, antigo e indisponível |
| Administrador | totais de pedidos, análises por resultado, regras com mais pendências |

## Estados do pedido que o app precisa mostrar (contrato existente)

`RASCUNHO` · `PROCESSANDO` (não equivale a concluído) · `REQUER_CONFERENCIA` · `COM_PENDENCIAS` · `SEM_PENDENCIAS_IDENTIFICADAS` (não é aprovação do SUS) · `FALHA_ANALISE` · `SUBMETIDA_SIMULACAO` (final). Fonte: `docs/api/estados-e-revisoes.md`.
