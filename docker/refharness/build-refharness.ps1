# Build + run the reference differential-test harness inside f2j-ref.
# Run from repo root: powershell -File docker/refharness/build-refharness.ps1
# Output binary: docker/refharness/build/refharness ; vectors printed to stdout.
param(
    [string]$GameEnDir = "C:\Work\f2j-gamedata\game-en"
)
$RepoRoot = (Get-Location).Path
docker run --rm -v "${RepoRoot}:/src" -v "${GameEnDir}:/game-en:ro" -v "f2j-ccache:/ccache" f2j-ref bash /src/docker/refharness/build.sh
