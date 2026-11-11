# Banco de dados

**PostgreSQL 18.6 via Docker** (confirmado). O esquema será gerenciado por scripts em `scripts/`; o Hibernate está com `ddl-auto: none`.

## Subir / parar (a partir de `database/`)
PowerShell:
```powershell
$env:DB_PASSWORD="escolha-uma-senha-de-dev"
docker compose up -d
docker compose stop      # para, preservando os dados
```
Linux/macOS: `export DB_PASSWORD=...` e os mesmos comandos `docker compose`.

- Porta no host: **5440** (5432 é usada por um PostgreSQL instalado nesta máquina; 5433/5434 também estavam ocupadas).
- Dados no volume `regulacheck-pgdata`. **Não use `docker compose down -v`**: apaga os dados.
- Use a mesma `DB_PASSWORD` ao subir o backend. O banco só aceita conexões de 127.0.0.1.
- Se o volume já existir, a senha original continua valendo; mudar `DB_PASSWORD` depois não a altera.
