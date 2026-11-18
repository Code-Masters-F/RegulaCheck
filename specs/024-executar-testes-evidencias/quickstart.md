# Quickstart: executar a rodada e conferir (RC-25)

Referências: plano `docs/testes/plano-de-testes.md` (seção 5 para ambiente, 5.4 para PB-1..PB-7) e contrato em `contracts/formato-evidencia-e-registro.md`.

## 1. Gate de ambiente (Onda 0)

```bash
git rev-parse --short HEAD && git status --short      # versão testada
java -version                                         # 21+ (backend); app usa JDK 21 do Android Studio
cd backend && ./mvnw.cmd clean verify                 # PowerShell: .\mvnw.cmd clean verify
curl -s http://localhost:8080/health                  # esperado {"status":"UP"}
```

O responsável sobe a API com `DB_*`, `DOCUMENT_STORAGE_PATH` e `IOT_API_KEY` (valores fora do repositório). Sem banco, a API responde 503 `DEPENDENCIA_INDISPONIVEL` em `/auth/*`, `/protocolos` e `/solicitacoes`: registrar a tentativa e marcar os casos afetados como Bloqueado.

Localizar o `adb` (Android SDK) e o emulador; se ausentes, os casos do app ficam Bloqueados após tentativa.

## 2. Executar um caso

1. Abrir o caso no plano; seguir seus Passos e PB-1..PB-7.
2. Criar `docs/evidencias/<ID>/` com `registro.md` (modelo do contrato) e `bruto/`.
3. Mascarar token, senha, chave e `DB_*` antes de gravar.
4. Conferir **todos** os itens do esperado; decidir o status.
5. Preencher Ambiente, Resultado obtido, Status, Evidência, Responsável e data no plano.

## 3. Verificar a rodada (leitura e busca)

```bash
# pastas de evidência e casos com status diferente de "Não executado"
ls docs/evidencias | grep -E '^(T[0-9]{2}-[0-9]{2}|P01)$' | wc -l
grep -c '| Status | Aprovado |'      docs/testes/plano-de-testes.md
grep -c '| Status | Reprovado |'     docs/testes/plano-de-testes.md
grep -c '| Status | Bloqueado |'     docs/testes/plano-de-testes.md
grep -c '| Status | Não executado |' docs/testes/plano-de-testes.md   # soma dos quatro = 94

# segredos (esperado: nenhuma ocorrência real)
grep -rn "Bearer [A-Za-z0-9._-]\{20,\}\|DB_PASSWORD=\|X-Chave-Simulador: [A-Za-z0-9]" docs/evidencias docs/testes
```

`git grep` só vê arquivos rastreados; use `grep` simples antes do commit.

## 4. Sinais de sucesso

- Os 16 cenários têm ao menos um caso executado ou um motivo registrado.
- Pelo menos 5 pastas de evidência completas; o P01 completo com a cadeia upload → leitura simulada → regras → resumo simulado → correção → reanálise → submissão simulada.
- Soma dos status = 94; todo Aprovado tem pasta; todo Bloqueado tem tentativa.
- Nenhum segredo nas evidências; revisão por outro integrante continua **pendente** no plano e no resumo.
