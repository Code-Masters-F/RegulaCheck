# Quickstart: verificar a RC-12 (motor de regras)

Não há rota HTTP nesta história: a verificação é por testes automatizados. Comandos a partir de `backend/` (Windows: `.\mvnw.cmd`).

## 1. Sem banco (obrigatório)

```bash
./mvnw -q -Dtest='CasosDoProtocoloTest,MotorDeRegrasTest,Regra*Test,EvidenciasDaLeituraTest,AvaliacaoServiceTest,HigieneDeLogAvaliacaoTest' test
./mvnw clean verify
```

Esperado: todos verdes; os testes Oracle aparecem como ignorados.

## 2. Gabarito independente dos 5 casos do protocolo

Montado à mão a partir de `docs/protocolo-academico.md` (não a partir do código). Data de referência 2026-11-13. Base: encaminhamento (ENCAMINHAMENTO, "Maria Exemplo da Silva", 1990-04-12, motivo preenchido, emissão 2026-10-20, autorização não localizada) e identificação (IDENTIFICACAO, mesmo nome e data; motivo, emissão e autorização não localizados), **todos os campos identificados confirmados**.

| # | Variação sobre a base | R01 | R02 | R03 | R04 | R05 | R06 | Geral |
|---|---|---|---|---|---|---|---|---|
| 1 | nenhuma | ATENDE | ATENDE | ATENDE | ATENDE | ATENDE | NAO_APLICAVEL | SEM_PENDENCIAS_IDENTIFICADAS |
| 2 | motivo do encaminhamento não localizado | ATENDE | ATENDE | ATENDE | PENDENTE | ATENDE | NAO_APLICAVEL | COM_PENDENCIAS |
| 3 | identificação não lida (leitura vazia) | ATENDE | NAO_VERIFICAVEL | NAO_VERIFICAVEL | ATENDE | ATENDE | NAO_VERIFICAVEL | REQUER_CONFERENCIA |
| 4 | nascimento 2011-05-01 nos dois documentos (15 anos) | ATENDE | ATENDE | ATENDE | ATENDE | ATENDE | PENDENTE | COM_PENDENCIAS |
| 5 | nome da identificação "Mariana Exemplo Souza" | ATENDE | PENDENTE | ATENDE | ATENDE | ATENDE | NAO_APLICAVEL | COM_PENDENCIAS |

Pendências esperadas: caso 2 → R04 (unidade emissora); caso 3 → R02 (`DOCUMENTO_NAO_LIDO`), R03 (`DOCUMENTO_NAO_LIDO`), R06 (`CONDICAO_INDETERMINADA`), todas da paciente; caso 4 → R06 (paciente); caso 5 → R02 (paciente).

## 3. Com as amostras sintéticas (montagem a partir do pedido)

`AvaliacaoServiceTest` sobe a API com repositórios em memória, envia `samples/encaminhamento-sintetico.pdf` e `samples/identificacao-sintetica.png`, pede a leitura (simulada) e:

- sem confirmar nada → `REQUER_CONFERENCIA`, motivos `TRANSCRICAO_NAO_CONFERIDA`;
- confirmando todos os campos identificados → `SEM_PENDENCIAS_IDENTIFICADAS`;
- estado, revisão, leitura e decisões iguais antes e depois; duas avaliações iguais;
- pedido da paciente B ou inexistente → mesma recusa 404; sem leitura ou leitura desatualizada → 409.

## 4. Oracle (opt-in, só com autorização)

Lê as regras da v0.1 em `RC_REGRA` e confere que o motor as aceita (sem script novo):

```bash
export DB_URL=... DB_USERNAME=... DB_PASSWORD=...   # credenciais do grupo, fora do repositório
RC_TESTE_ORACLE=SIM ./mvnw -q -Dtest=RegrasDoProtocoloOracleTest test
```

Sem `RC_TESTE_ORACLE=SIM`, o teste é ignorado e deve ser registrado como **não verificado**.
