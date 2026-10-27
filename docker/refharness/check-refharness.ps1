# Rebuild the harness, re-run it and diff against the golden vectors.
# Run from repo root: powershell -File docker/refharness/check-refharness.ps1
# Exit code 0 = vectors match golden/v175.txt (v1-v174 are earlier snapshots,
# kept for history; note v1's INFO line sits on top due to pre-v2 buffering).
param(
    [string]$GameEnDir = "C:\Work\f2j-gamedata\game-en"
)
$RepoRoot = (Get-Location).Path
docker run --rm -v "${RepoRoot}:/src" -v "${GameEnDir}:/game-en:ro" -v "f2j-ccache:/ccache" f2j-ref bash /src/docker/refharness/check.sh
