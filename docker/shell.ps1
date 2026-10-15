# Interactive shell in the reference container (repo root mounted at /src).
# Run from repo root: powershell -File docker/shell.ps1
$RepoRoot = (Get-Location).Path
docker run --rm -it `
    -v "${RepoRoot}:/src" `
    -v "f2j-ccache:/ccache" `
    f2j-ref bash
