# Implementation Plan: Proteger entradas e processamento (RC-06)

**Branch**: `006-protecao-entradas` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-protecao-entradas/spec.md`

## Summary

Entrega as **proteções transversais reutilizáveis** que as histórias de envio, extração e resumo vão chamar, sem criar rotas de negócio. Seis peças:

1. **Conferência de arquivo** (`ConferenciaDeArquivo`): copia o fluxo recebido para uma área de quarentena com limite rígido de 5 MB, descobre o **tipo real** pelos primeiros bytes (PDF/JPEG/PNG), cruza com a extensão, conta páginas do PDF e detecta PDF protegido (Apache PDFBox, única dependência nova) e lê só o cabeçalho da imagem (`ImageIO`) para limitar dimensões. Resultado: `Aceito` ou `Recusado(motivo)` de uma lista fechada. A quantidade (≤ 3) é conferida por `LimitesArquivo.exigirQuantidade`.
2. **Armazenamento privado** (`ArmazenamentoPrivado` + `ArmazenamentoEmDisco`): grava o arquivo já conferido em `DOCUMENT_STORAGE_PATH` com **nome interno aleatório** (32 hex), de forma atômica; só abre por esse nome, resolvido e comparado com a raiz (sem `..`, sem link simbólico para fora). O nome enviado vira apenas texto de exibição saneado (`NomeExibicao`).
3. **Download seguro** (`DownloadSeguro`): grava o fluxo na resposta como **anexo**, com tipo vindo da conferência, `nosniff` e sem cache. A autorização é a da RC-05 (`RotasPrivadas` com `PACIENTE` + `Propriedade`).
4. **Cabeçalhos e texto puro** (`CabecalhosSegurosFiltro`, `Json` com escape de `<`, `>`, `&`, `'`): todas as respostas saem com `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` e CSP restritiva; o JSON nunca carrega `<` ou `>` literais, de modo que o texto do resumo simulado é inerte mesmo que um cliente o trate como página.
5. **Conteúdo não confiável** (`TextoNaoConfiavel`, `ValidadorDeSaidaEstruturada`): o texto de documento é carregado num tipo que não se imprime em log nem vira campo de controle, entra em qualquer mensagem a outro componente **delimitado e identificado**, e a saída de extração/resumo só é aceita se obedecer a um contrato fechado (campos conhecidos, códigos de regra e estados de listas fixas).
6. **Segredos e canal** (`VarreduraDeSegredos` + teste, `RedatorDeLog`, `ConfiguracaoDeCanal`): varredura repetível sobre o repositório e `mobile/`; mascaramento de mensagens de exceção antes de ir ao log; `API_PUBLIC_URL` opcional que **recusa subir** se apontar para endereço remoto sem `https`.

Mais uma conferência automática de **SQL parametrizado** (varredura de código-fonte) e a documentação (guia de uso, contrato de erros 400/413/415, variáveis). **Nenhuma tabela nova**: `RC_DOCUMENTO` (RC-03) já tem `TIPO`, `TAMANHO_BYTES`, `PAGINAS` e `CAMINHO_INTERNO` com as mesmas restrições (≤ 5 MB, PDF ≤ 5 páginas). Nenhuma operação no Oracle.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona), como nas histórias anteriores.

**Primary Dependencies**: **uma nova**, `org.apache.pdfbox:pdfbox` 3.0.x (versão estável fixa, confirmada no Maven Central na T001; candidata 3.0.8) para contar páginas e detectar PDF protegido de forma confiável. Alternativa de contar `/Type /Page` por texto foi rejeitada (falsificável; ver research D2). Reaproveita Jetty 12 EE10 Servlet, Jackson, SLF4J e JUnit já no `pom.xml`. Sem Spring/JPA.

**Storage**: arquivos em disco local sob `DOCUMENT_STORAGE_PATH` (variável já reservada, opcional para subir). Metadados: colunas já existentes de `RC_DOCUMENTO`; a gravação dessas linhas é da história de envio. Sem script SQL novo.

**Testing**: JUnit, Jetty real em porta livre, repositórios em memória, diretório temporário por teste (`@TempDir`), arquivos **gerados em código** (PDFs mínimos, PNG/JPEG de poucos bytes, ZIP/EXE/texto disfarçados) mais um pequeno conjunto em `backend/src/test/resources`. `mvnw clean verify` sem banco. O único teste contra Oracle é opt-in e somente leitura (consulta de login com `' OR 1=1 --`).

**Target Platform**: JVM 21+ em Windows/Linux (nomes reservados do Windows e separadores de ambos os sistemas são tratados).

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: conferência de um arquivo de 5 MB < 1 s; leitura em fluxo com buffer fixo (sem carregar o arquivo inteiro na memória, exceto o que o PDFBox mantém por página); suíte das proteções < 60 s (SC-009).

**Constraints**: sem Spring/JPA; negar por omissão; nenhum log com senha, token, chave, cadeia de conexão ou documento completo; respostas no formato padrão de erro; a API continua subindo sem `DB_*`, sem `IOT_API_KEY` e sem `DOCUMENT_STORAGE_PATH`; nada de conteúdo ativo de arquivo é executado.

**Scale/Scope**: ~16 classes novas (pacotes `armazenamento`, `armazenamento/conferencia`, `common/http`, `common/log`, `integracao`), 1 dependência, 1 variável nova opcional (`API_PUBLIC_URL`), ~12 classes de teste, documentação e guia. Dados 100% sintéticos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: arquivos e textos de teste são gerados/sintéticos; nada de documento real. |
| II. Limites clínicos e integridade | Atende: a conferência não interpreta conteúdo clínico nem altera o documento original (guarda a cópia byte a byte). |
| III. Monólito modular Java sem Spring/JPA | Atende: classes simples nos pacotes previstos (`armazenamento`, `common`, `integracao`); Controller não acessa Repository; a única interface nova é a de fronteira de armazenamento (CLAUDE.md §6 pede "contrato e implementação privada"). Uma biblioteca nova, justificada em research D2 e registrada em `docs/decisoes.md`. |
| IV. Segurança e autorização por recurso | É a história que implementa tipo real/tamanho/páginas, armazenamento privado, download autenticado, segredos fora de repo/log e consultas parametrizadas; o download reusa `RotasPrivadas`/`Propriedade` da RC-05. |
| V. Regras determinísticas, IA subordinada, dados não confiáveis | Atende: texto de documento é dado; saída fora do contrato é rejeitada; IA/extração continuam mockadas e identificadas (nenhum provedor real). |
| VI. Persistência e confiabilidade | Atende: gravação atômica (quarentena → rename), sem arquivo parcial; nenhuma alteração de esquema; transação não envolve chamada externa. |
| VII. Verificação honesta | Atende: cada requisito tem teste ou verificação repetível; limites e não verificados listados (sem upload real, sem app, HTTPS por configuração). |
| VIII. Código documentado, mudanças mínimas | Atende: JavaDoc em português; alterações em arquivos existentes limitadas a `Json`, `ServletJson`/`Composicao` (filtro de cabeçalhos), `RespostaErro` (log), `Configuracao`. |
| Restrições tecnológicas | Atende: versão fixa, sem SNAPSHOT; variáveis novas opcionais para iniciar. |

**Re-avaliação após o desenho (Phase 1)**: sem violações. Complexity Tracking registra apenas a dependência PDFBox.

## Project Structure

### Documentation (this feature)

```text
specs/006-protecao-entradas/
├── plan.md              # Este arquivo
├── research.md          # Decisões D1–D12
├── data-model.md        # Modelo das proteções (sem tabelas novas)
├── quickstart.md        # Como verificar e como proteger uma rota nova
├── contracts/
│   └── api-protecoes.md # Erros 400/413/415, download, cabeçalhos, limites
├── checklists/
│   └── requirements.md
└── tasks.md             # Gerado por /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── armazenamento/
│   ├── ArmazenamentoPrivado.java          # contrato: guardar(ArquivoConferido) / abrir(idInterno) / remover
│   ├── ArmazenamentoEmDisco.java          # implementação: quarentena + rename atômico; raiz de DOCUMENT_STORAGE_PATH
│   ├── ArquivoGuardado.java               # record: idInterno, tipo, tamanho, páginas, nomeExibicao
│   ├── IdInterno.java                     # 32 hex aleatórios; valida formato (impede travessia por construção)
│   ├── NomeExibicao.java                  # sanea o nome original para exibição (nunca vira caminho)
│   └── conferencia/
│       ├── ConferenciaDeArquivo.java      # fluxo -> quarentena (limite rígido) -> tipo real -> extensão -> páginas
│       ├── TipoArquivo.java               # PDF, JPEG, PNG: assinatura, extensões, MIME
│       ├── MotivoRecusa.java              # lista fechada -> status HTTP, código e mensagem
│       ├── ResultadoConferencia.java      # Aceito(ArquivoConferido) | Recusado(MotivoRecusa)
│       ├── ArquivoConferido.java          # caminho temporário + tipo + tamanho + páginas
│       ├── LimitesArquivo.java            # 3 arquivos, 5 MB, 5 páginas, dimensões máximas (constantes centrais)
│       └── LeitorDePaginas.java           # PDFBox: contagem e detecção de proteção; só leitura
├── common/
│   ├── http/
│   │   ├── CabecalhosSegurosFiltro.java   # nosniff, no-store, CSP, Referrer-Policy (todas as respostas)
│   │   └── DownloadSeguro.java            # anexo + tipo da conferência + nosniff; fluxo em buffer
│   ├── log/RedatorDeLog.java              # mascara senha, token, chave, jdbc user/senha em mensagens de exceção
│   └── Json.java                          # escape de < > & ' nas respostas (CharacterEscapes)
├── integracao/
│   ├── TextoNaoConfiavel.java             # record: texto de documento; toString sem conteúdo; delimitar()
│   ├── ValidadorDeSaidaEstruturada.java   # contrato fechado de extração/resumo; rejeita campo/código/estado fora da lista
│   └── ia/TextoSimulado.java              # record: texto, simulado=true, formato="texto_puro" (DTO do resumo simulado)
├── common/exception/RespostaErro.java     # log da falha inesperada passa pelo RedatorDeLog
└── config/
    ├── Configuracao.java                  # + armazenamentoRaiz (DOCUMENT_STORAGE_PATH), + urlPublica (API_PUBLIC_URL)
    ├── ConfiguracaoDeCanal.java           # recusa urlPublica remota sem https
    └── Composicao.java                    # registra CabecalhosSegurosFiltro em /*; expõe ArmazenamentoPrivado

backend/src/test/java/br/com/regulacheck/
├── armazenamento/conferencia/ConferenciaDeArquivoTest (tipo real, PDF protegido, imagem e conteúdo ativo consolidados), LimitesTest, MotivoRecusaTest, PedidoComVariosArquivosTest
├── armazenamento/ArmazenamentoEmDiscoTest, NomeMaliciosoTest, GravacaoAtomicaTest
├── armazenamento/DownloadAutenticadoTest              # dona / alheia / admin / simulador / sem token, via RotasPrivadas
├── common/http/CabecalhosSegurosTest, TextoPuroTest
├── integracao/ConteudoNaoConfiavelTest, SaidaForaDoContratoTest, InjecaoEmDocumentoTest
├── seguranca/VarreduraDeSegredosTest, SqlParametrizadoTest, HigieneDeLogGeralTest, ConfiguracaoDeCanalTest
└── (fixtures) ArquivosDeTeste (gera PDFs/imagens/disfarçados em memória), RotasDeArquivoDeExemplo (só teste)

backend/src/test/resources/arquivos/              # poucos arquivos binários mínimos e inofensivos (ex.: PDF protegido)
docs/
├── api/{erros.md, protecoes.md (guia), README.md, openapi.yaml, contratos-dados.md}
├── {ambiente.md, arquitetura.md, decisoes.md}
CONTRATO_API.md, .env.example, mobile/README.md, backend/pom.xml
```

**Structure Decision**: segue `CLAUDE.md` §6 (`armazenamento` = contrato e implementação privada; `common` para o que é transversal; `integracao` para os pontos de contrato de IA/extração). A conferência fica em subpacote de `armazenamento` porque só existe para decidir se um arquivo pode ser guardado. Nenhum módulo Maven novo; sem `utils`.

## Complexity Tracking

| Desvio | Por que é necessário | Alternativa simples rejeitada |
|---|---|---|
| Dependência `pdfbox` (e a transitiva `pdfbox-io`, `fontbox`, `commons-logging`) | Contar páginas e detectar proteção de PDF de forma confiável; PDF malformado pode enganar contagem por texto | Contagem por expressão regular no texto bruto: falsificável e falha em PDFs com fluxos compactados |
