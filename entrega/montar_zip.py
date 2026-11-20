"""Monta o ZIP de entrega. Uso, da raiz do repositorio: uv run python -X utf8 entrega/montar_zip.py

Inclui o que o Git versiona ou ainda nao ignorou (git ls-files -co --exclude-standard), menos o que nao vai no pacote:
specs/, .specify/, entrega/prints/, pastas de agentes de IA, PDF do enunciado, builds . O .env nunca entra (esta no .gitignore)."""
import subprocess, zipfile, pathlib, re, sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
SAIDA = RAIZ.parent / "RegulaCheck-CodeMasters.zip"
FORA = re.compile(r"^(specs/|\.specify/|entrega/prints/|\.claude/|\.agents/|\.serena/|AGENTS\.md$|3ESOA[^/]*\.pdf$|\.git/|entrega/montar_zip\.py$)|(^|/)(build|target|node_modules|\.gradle|\.idea)/|(^|/)\.env(\.(?!example$)[^/]*)?$|local\.properties$")

arqs = subprocess.run(["git", "ls-files", "-co", "--exclude-standard"], cwd=RAIZ, capture_output=True, text=True, check=True).stdout.splitlines()
arqs = sorted(a for a in arqs if not FORA.search(a) and (RAIZ / a).is_file())
if SAIDA.exists():
    SAIDA.unlink()
with zipfile.ZipFile(SAIDA, "w", zipfile.ZIP_DEFLATED) as z:
    for a in arqs:
        z.write(RAIZ / a, "RegulaCheck/" + a)
print(f"ZIP: {SAIDA}  ({len(arqs)} arquivos, {SAIDA.stat().st_size/1e6:.1f} MB)")
