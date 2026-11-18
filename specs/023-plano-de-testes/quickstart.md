# Quickstart: revisar o plano e usá-lo (RC-24)

## Para revisar (outro integrante)

1. Abra `docs/testes/plano-de-testes.md`.
2. Escolha **um caso por grupo** (funcionamento, falha, segurança) e confira, sem ajuda do autor: dá para saber a conta, o estado do pedido, o arquivo e o resultado esperado?
3. Confira que todo caso está "Não executado" e com resultado, evidência, responsável e data em branco.
4. Abra três entradas documentais e confirme que o arquivo e o `gabarito.md` existem em `samples/conjuntos/`.
5. Rode `grep -nE "extração real|resumo real|IA real" docs/testes/plano-de-testes.md`: só deve aparecer em contexto de "a issue diz… o contrato é simulado".
6. Registre a revisão no plano (seção "Status do plano"); enquanto isso não ocorrer, ela continua **pendente**.

## Para executar (RC-25)

1. Siga a seção "Ambiente e preparação" até a API responder em `/health`.
2. Execute o caso, guarde o material em `docs/evidencias/<ID-DO-CASO>/` e preencha resultado obtido, status, responsável e data.
3. Divergência = **Reprovado** com achado; impedimento de ambiente ou funcionalidade pendente = **Bloqueado** com motivo; nunca edite o resultado esperado para coincidir.

## Conferência desta feature (sem executar caso)

```bash
# grep comum (não git grep): o plano ainda não está commitado
# a partir da raiz: todo caso começa Não executado, sem aprovado
grep -c "Não executado" docs/testes/plano-de-testes.md
grep -nE "\| Status \| (Aprovado|Reprovado|Bloqueado) \|" docs/testes/plano-de-testes.md   # deve não retornar nada
# referências a amostras existem
grep -ohE "samples/[A-Za-z0-9_./-]+" docs/testes/plano-de-testes.md | sort -u | while read f; do [ -e "$f" ] || echo "FALTA $f"; done
```
