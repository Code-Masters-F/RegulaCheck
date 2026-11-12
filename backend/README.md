# Backend

API em **Java puro** (sem Spring e sem JPA/Hibernate em produção): Jakarta Servlet em Jetty embutido, módulo Maven único, pacotes por funcionalidade em `br.com.regulacheck`.

## Estrutura
- `RegulaCheckApplication`: ponto de entrada.
- `config/`: `Configuracao` (variáveis de ambiente opcionais), `Composicao` (montagem manual das dependências, sem container de injeção) e `Servidor` (Jetty).
- `common/`: `Json`, tratamento padrão de erros (`exception/`) e base dos controllers (`http/`).
- `saude/`: exemplo mínimo de Controller → Service → Repository para `GET /health`; **não** é funcionalidade de negócio.
- Demais pacotes (`auth`, `usuario`, `solicitacao`, `documento`, ...): só `package-info.java`, sem classes ainda.

## Executar e testar (a partir de `backend/`)
Pré-requisito: JDK 21 ou superior (verificado com 25.0.2). Nenhum banco nem variável é necessária.
```powershell
.\mvnw.cmd clean verify
java -jar target\regulacheck-backend.jar
curl http://localhost:8080/health
```
Linux/macOS: `./mvnw clean verify` e `java -jar target/regulacheck-backend.jar`. Variáveis opcionais: `SERVER_PORT` (padrão 8080) e `SERVER_HOST` (padrão `0.0.0.0`).

Contratos e erros: [docs/api](../docs/api/README.md). Ambiente: [docs/ambiente.md](../docs/ambiente.md).
