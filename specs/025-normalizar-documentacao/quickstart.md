# Quickstart: executar a limpeza

1. Baseline (a partir de `backend/`): `.\mvnw.cmd clean verify`; registrar resultado e tempo. Registrar o hash de `docs/api/openapi.yaml` e a contagem de links quebrados.
2. Gerar o inventário de documentos rastreados (`git ls-files`, excluindo os protegidos de [research.md](research.md) D6) e preencher `inventario.md` conforme [data-model.md](data-model.md).
3. Declarar a fonte de verdade de cada tema e decidir destino (critério D2).
4. Aplicar as ondas de limpeza em ordem, rodando a verificação de links ao fim de cada onda.
5. Registrar decisões em `docs/decisoes.md` e atualizar os índices.
6. Repetir as verificações de [contracts/verificacao.md](contracts/verificacao.md) e registrar os resultados reais.

Não commitar sem pedido. Scripts auxiliares ficam fora do repositório.
